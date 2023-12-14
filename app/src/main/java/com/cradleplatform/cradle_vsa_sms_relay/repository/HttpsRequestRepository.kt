package com.cradleplatform.cradle_vsa_sms_relay.repository

import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.service.SMSRelayService
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
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
        smsRelayService.postSMSRelay(httpsRequest).enqueue(object : Callback<HTTPSResponse> {
            override fun onResponse(
                call: Call<HTTPSResponse>,
                response: retrofit2.Response<HTTPSResponse>
            ) {
                if(response.isSuccessful){
                    val httpsResponse = response.body()
                    if (httpsResponse != null) {
                        // using a synchronized block to ensure no two threads
                        // can execute this block at the same time
                        // this is a safety measure, only for a special case where a user attempts
                        // to manually restart an incomplete relay process
                        synchronized(this@HttpsRequestRepository){
                            processSuccessfulCallback(smsRelayEntity, httpsResponse)
                        }
                    }
                    else{
                        //generic error
                    }
                }
                else{
                    // generic error message
                }
            }

            override fun onFailure(call: Call<HTTPSResponse>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun processSuccessfulCallback(smsRelayEntity: SmsRelayEntity, httpsResponse: HTTPSResponse) {

        val phoneNumber: String = smsRelayEntity.getPhoneNumber()
        val requestCounter: String = smsRelayEntity.getRequestIdentifier()

        val smsMessages = smsFormatter.formatSMS(
            httpsResponse.body,
            requestCounter.toLong(),
            true
        )
        val firstMessage = smsMessages.removeAt(0)

        smsRelayEntity.isServerError = false
        smsRelayEntity.isServerResponseReceived = true
        smsRelayEntity.smsPackets = smsMessages
        smsRelayEntity.totalFragmentsFromMobile = smsMessages.size + 1
        smsRelayEntity.numFragmentsSentToMobile = 1
        smsRelayEntity.timeLastDataMessageSent = System.currentTimeMillis()

        smsRelayRepository.updateBlocking(smsRelayEntity)

        smsFormatter.sendMessage(phoneNumber, firstMessage)
    }

    private fun processUnsuccessfulCallback(){

    }
}
