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

private class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

class HttpsRequestRepository(
    token: String,
    private val smsFormatter: SMSFormatter,
    private val smsRelayRepository: SmsRelayRepository
) {
    // Todo remove hardcoding for base url
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
        val httpsRequest = HTTPSRequest(smsRelayEntity.getPhoneNumber(), smsRelayEntity.encryptedDataFromMobile)
        smsRelayService.postSMSRelay(httpsRequest).enqueue(
            object : Callback<HTTPSResponse> {
            override fun onResponse(
                call: Call<HTTPSResponse>,
                response: retrofit2.Response<HTTPSResponse>
            ) {
                if(response.isSuccessful) {
                    val httpsResponse = response.body()
                    if (httpsResponse != null) {
                        // using a synchronized block to ensure no two threads
                        // can execute this block at the same time
                        // this is a safety measure, only for a special case where a user attempts
                        // to manually restart an incomplete relay process
                        synchronized(this@HttpsRequestRepository) {
                            updateSmsRelayEntity(httpsResponse.body, true, smsRelayEntity)
                        }
                        return
                    }
                }
                val errorBody = response.errorBody()?.string()
                val errorMessage : String
                if (errorBody == null){
                    errorMessage = "There was an unexpected error while sending the relay request - Status ${response.code()}"
                }
                // expected errors will be inside a json with a key message
                else{
                    val errorJson = JSONObject(errorBody)
                    errorMessage = errorJson.getString("message")
                }
                synchronized(this@HttpsRequestRepository) {
                    updateSmsRelayEntity(errorMessage, false, smsRelayEntity)
                }
            }

            override fun onFailure(call: Call<HTTPSResponse>, t: Throwable) {
                // TODO - Implement what happens when request fails because of network errors
                // This method will only be called when there is a network error
            }
        })
    }

    private fun updateSmsRelayEntity(data: String, isSuccessful: Boolean, smsRelayEntity: SmsRelayEntity){
        val phoneNumber: String = smsRelayEntity.getPhoneNumber()
        val requestCounter: String = smsRelayEntity.getRequestIdentifier()

        val smsMessages = smsFormatter.formatSMS(
            data,
            requestCounter.toLong(),
            isSuccessful
        )

        val firstMessage = smsMessages.removeAt(0)

        smsRelayEntity.isServerError = isSuccessful
        smsRelayEntity.isServerResponseReceived = true
        smsRelayEntity.smsPackets = smsMessages
        smsRelayEntity.totalFragmentsFromMobile = smsMessages.size + 1
        smsRelayEntity.numFragmentsSentToMobile = 1
        smsRelayEntity.timeLastDataMessageSent = System.currentTimeMillis()

        smsRelayRepository.updateBlocking(smsRelayEntity)

        smsFormatter.sendMessage(phoneNumber, firstMessage)
    }
}
