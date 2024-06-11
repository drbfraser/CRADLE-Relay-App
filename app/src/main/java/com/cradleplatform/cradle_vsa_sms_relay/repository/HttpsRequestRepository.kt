package com.cradleplatform.cradle_vsa_sms_relay.repository

import android.util.Log
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponseSent
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.service.SMSRelayService
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * class to add auth token to requests sent to the server
 */
private class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

/**
 * class to make requests to the server
 * class registers callbacks for when the response is received from the server
 * callbacks process the response and initiate the sms protocol to send data to mobile
 */

class HttpsRequestRepository(
    token: String,
    private val smsFormatter: SMSFormatter,
    private val smsRelayRepository: SmsRelayRepository,
    baseUrl: String
) {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(token))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val smsRelayService = retrofit.create(SMSRelayService::class.java)

    private val responseFailures: ConcurrentHashMap<SmsRelayEntity, Pair<String, Int>> =
        ConcurrentHashMap()

    private val retryQueue: PriorityBlockingQueue<Triple<SmsRelayEntity, Long, CoroutineScope>> =
        PriorityBlockingQueue(
            PRIORITY_QUEUE_INIT_CAP
        ) { i, j -> i.second.compareTo(j.second) }
    private val scheduler = Executors.newScheduledThreadPool(1)

    private val _events = MutableSharedFlow<Pair<SmsRelayEntity, HTTPSResponseSent>>()
    val events = _events.asSharedFlow()

    init {
        scheduler.scheduleWithFixedDelay(
            { retrySendMessageToHTTPServer() },
            0,
            RETRY_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    fun sendToServer(smsRelayEntity: SmsRelayEntity, coroutineScope: CoroutineScope) {
        val encryptedData = smsFormatter.getEncryptedData(smsRelayEntity)
        val httpsRequest = HTTPSRequest(smsRelayEntity.getPhoneNumber(), encryptedData)

        retryQueue.add(
            Triple(
                smsRelayEntity,
                System.currentTimeMillis() + min(
                    DEFAULT_WAIT * 2.0.pow(smsRelayEntity.numberOfTriesUploaded),
                    MAX_WAIT
                ).toLong(),
                coroutineScope
            )
        )

        smsRelayService.postSMSRelay(httpsRequest).enqueue(
            object : Callback<HTTPSResponse> {
                override fun onResponse(
                    call: Call<HTTPSResponse>,
                    response: retrofit2.Response<HTTPSResponse>
                ) {
                    if (response.isSuccessful) {
                        val httpsResponse = response.body()
                        if (httpsResponse != null) {
                            responseFailures.remove(smsRelayEntity)
                            // using a synchronized block to ensure no two threads
                            // can execute this block at the same time
                            // this is a safety measure, only for a special case where a user attempts
                            // to manually restart an incomplete relay process
                            synchronized(this@HttpsRequestRepository) {
                                updateSmsRelayEntity(
                                    httpsResponse.body,
                                    true,
                                    smsRelayEntity,
                                    response.code(),
                                    coroutineScope
                                )
                            }
                            return
                        }
                    }
                    val errorBody = response.errorBody()
                    val errorMessage = if (errorBody == null) {
                        "There was an unexpected error while sending the relay request - Status ${response.code()}"
                    }
                    // expected errors will be inside a json which will contain the key 'message'
                    else {
                        val errorBodyString = errorBody.string()
                        try {
                            JSONObject(errorBodyString).getString("message")
                        } catch (e: JSONException) {
                            "Unexpected error format: $errorBodyString"
                        }
                    }
                    // Add retry functionality here as well and look at why we are doing the
                    //  below on failure
                    Log.e(TAG, errorMessage)

                    responseFailures[smsRelayEntity] = Pair(errorMessage, response.code())
                }

                // This method will only be called when there is a network error while uploading
                override fun onFailure(call: Call<HTTPSResponse>, t: Throwable) {
                    Log.e(TAG, t.toString())
                    responseFailures[smsRelayEntity] =
                        Pair(GENERIC_NETWORK_ERROR_MESSAGE, GENERIC_NETWORK_ERROR_STATUS_CODE)
                }
            }
        )
    }

    private fun updateSmsRelayEntity(
        data: String,
        isSuccessful: Boolean,
        smsRelayEntity: SmsRelayEntity,
        code: Int,
        coroutineScope: CoroutineScope
    ) {
        val phoneNumber: String = smsRelayEntity.getPhoneNumber()
        val requestCounter: String = smsRelayEntity.getRequestIdentifier()
        val smsMessages = smsFormatter.formatSMS(
            data,
            requestCounter.toLong(),
            isSuccessful,
            code
        )

        val firstMessage = smsMessages.removeAt(0)

        // Handles the case when there are no retry attempts
        smsRelayEntity.numberOfTriesUploaded = max(1, smsRelayEntity.numberOfTriesUploaded)

        smsRelayEntity.isServerError = !isSuccessful
        smsRelayEntity.isServerResponseReceived = isSuccessful
        smsRelayEntity.smsPacketsToMobile.addAll(smsMessages)
        smsRelayEntity.totalFragmentsFromMobile = smsMessages.size + 1
        smsRelayEntity.numFragmentsSentToMobile = 1
        smsRelayEntity.timestampsDataMessagesSent.add(System.currentTimeMillis())

        smsRelayRepository.updateBlocking(smsRelayEntity)

        smsFormatter.sendMessage(phoneNumber, firstMessage)
        coroutineScope.launch {
            publishEvent(
                Pair(
                    smsRelayEntity,
                    HTTPSResponseSent(smsRelayEntity.id, phoneNumber, firstMessage)
                )
            )
        }
    }

    private suspend fun publishEvent(event: Pair<SmsRelayEntity, HTTPSResponseSent>) {
        Log.d(TAG, "Publishing event with ${event.first.id}, ${event.second.phoneNumber}")
        _events.emit(event)
    }

    private fun retrySendMessageToHTTPServer() {
        val startExe = System.currentTimeMillis()
        while (retryQueue.peek()?.let { it.second <= startExe } == true) {
            val polledTriple = retryQueue.poll()
            if (
                polledTriple!!.first.numberOfTriesUploaded == MAX_RETRIES &&
                !polledTriple.first.isServerResponseReceived
            ) {
                Log.d(
                    TAG,
                    "Attempted sending ${polledTriple.first.id} $MAX_RETRIES times; dropping"
                )
                polledTriple.first.isCompleted = false
                smsRelayRepository.updateBlocking(polledTriple.first)
                val removed = responseFailures.remove(polledTriple.first)
                @Suppress("LoopWithTooManyJumpStatements")
                if (removed != null) {
                    synchronized(this@HttpsRequestRepository) {
                        updateSmsRelayEntity(
                            removed.first,
                            false,
                            polledTriple.first,
                            removed.second,
                            polledTriple.third
                        )
                    }
                } else {
                    synchronized(this@HttpsRequestRepository) {
                        updateSmsRelayEntity(
                            HTTP_RESPONSE_TIMEOUT_MESSAGE,
                            false,
                            polledTriple.first,
                            HTTP_RESPONSE_TIMEOUT_STATUS_CODE,
                            polledTriple.third
                        )
                    }
                }
            }
            if (polledTriple.first.isServerResponseReceived) {
                Log.d(TAG, "Already received ${polledTriple.first.id}; dropping")
            }
            if ((polledTriple.first.numberOfTriesUploaded == MAX_RETRIES &&
                        !polledTriple.first.isServerResponseReceived) || polledTriple.first.isServerResponseReceived
            ) {
                continue
            }
            Log.d(TAG, "Retrying send to ${polledTriple.first.id}")

            polledTriple.first.numberOfTriesUploaded += 1
            smsRelayRepository.updateBlocking(polledTriple.first)
            sendToServer(polledTriple.first, polledTriple.third)
        }
    }

    companion object {
        const val TAG = "HttpsRequestRepository"
        private const val PRIORITY_QUEUE_INIT_CAP = 11
        private const val MAX_RETRIES = 5
        private const val DEFAULT_WAIT = 3000L
        private const val MAX_WAIT = 30000.0
        private const val RETRY_CHECK_INTERVAL_MS: Long = 250
        private const val GENERIC_NETWORK_ERROR_MESSAGE = "There was a server-side error."
        private const val GENERIC_NETWORK_ERROR_STATUS_CODE = 500
        private const val HTTP_RESPONSE_TIMEOUT_MESSAGE = "Server took too long to respond."
        private const val HTTP_RESPONSE_TIMEOUT_STATUS_CODE = 504
    }
}
