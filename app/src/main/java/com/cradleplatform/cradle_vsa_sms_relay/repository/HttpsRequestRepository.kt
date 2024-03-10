package com.cradleplatform.cradle_vsa_sms_relay.repository

import android.util.Log
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.service.SMSRelayService
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

private const val MAX_UPLOAD_ATTEMPTS = 5

/**
 * class to make requests to the server
 * class registers callbacks for when the response is received from the server
 * callbacks process the response and initiate the sms protocol to send data to mobile
 */

class HttpsRequestRepository(
    token: String,
    private val smsFormatter: SMSFormatter,
    private val smsRelayRepository: SmsRelayRepository
) {

    // Todo remove hardcoding for base url - move to settings.xml
    private val baseUrl = "http://10.0.2.2:5000/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(token))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val smsRelayService = retrofit.create(SMSRelayService::class.java)

    fun sendToServer(smsRelayEntity: SmsRelayEntity) {
        val encryptedData = smsFormatter.getEncryptedData(smsRelayEntity)
        val httpsRequest = HTTPSRequest(smsRelayEntity.getPhoneNumber(), encryptedData)
        smsRelayService.postSMSRelay(httpsRequest).enqueue(
            object : Callback<HTTPSResponse> {
            override fun onResponse(
                call: Call<HTTPSResponse>,
                response: retrofit2.Response<HTTPSResponse>
            ) {
                if (response.isSuccessful) {
                    val httpsResponse = response.body()
                    if (httpsResponse != null) {
                        // using a synchronized block to ensure no two threads
                        // can execute this block at the same time
                        // this is a safety measure, only for a special case where a user attempts
                        // to manually restart an incomplete relay process
                        synchronized(this@HttpsRequestRepository) {
                            updateSmsRelayEntity(httpsResponse.body, true, smsRelayEntity, response.code())
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
                    JSONObject(errorBody.string()).getString("message")
                }
                synchronized(this@HttpsRequestRepository) {
                    updateSmsRelayEntity(errorMessage, false, smsRelayEntity, response.code())
                }
            }

            // This method will only be called when there is a network error while uploading
            override fun onFailure(call: Call<HTTPSResponse>, t: Throwable) {
                Log.e(TAG, t.toString())

                // max number of attempted uploads is 5
                // TODO remove hardcoding and move to settings.xml
                if (smsRelayEntity.numberOfTriesUploaded > MAX_UPLOAD_ATTEMPTS) {
                    smsRelayEntity.isServerError = true
                    smsRelayEntity.isServerResponseReceived = false
                    smsRelayEntity.isCompleted = false
                    smsRelayRepository.update(smsRelayEntity)
                    return
                }

                smsRelayEntity.numberOfTriesUploaded += 1
                smsRelayRepository.updateBlocking(smsRelayEntity)

                sendToServer(smsRelayEntity)
            }
        })
    }

    private fun updateSmsRelayEntity(data: String, isSuccessful: Boolean, smsRelayEntity: SmsRelayEntity, code: Int) {
        val phoneNumber: String = smsRelayEntity.getPhoneNumber()
        val requestCounter: String = smsRelayEntity.getRequestIdentifier()
        //val dateandTime : String = smsRelayEntity.getDateAndTime()

        val smsMessages = smsFormatter.formatSMS(
            data,
            requestCounter.toLong(),
            isSuccessful,
            code
        )

        val firstMessage = smsMessages.removeAt(0)

        smsRelayEntity.isServerError = !isSuccessful
        smsRelayEntity.isServerResponseReceived = true
        smsRelayEntity.smsPacketsToMobile.addAll(smsMessages)
        smsRelayEntity.totalFragmentsFromMobile = smsMessages.size + 1
        smsRelayEntity.numFragmentsSentToMobile = 1
        smsRelayEntity.timestampsDataMessagesSent.add(System.currentTimeMillis())

        smsRelayRepository.updateBlocking(smsRelayEntity)

        smsFormatter.sendMessage(phoneNumber, firstMessage)
    }

    companion object {
        const val TAG = "HttpsRequestRepository"
    }
}
