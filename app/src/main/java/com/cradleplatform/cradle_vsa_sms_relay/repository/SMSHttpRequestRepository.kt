package com.cradleplatform.cradle_vsa_sms_relay.repository

import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.cradleplatform.cradle_vsa_sms_relay.service.SMSRelayService
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

class SMSHttpRequestRepository(
    token: String
) : SMSHttpRequestRepositoryInterface {

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

    override fun sendSMSHttpRequestToServer(smsHttpRequest: SMSHttpRequest): Call<HTTPSResponse> {
        val httpsRequest = SMSFormatter.convertSMSHttpRequestToHttpsRequest(smsHttpRequest)
        return smsRelayService.postSMSRelay(httpsRequest)
    }
}
