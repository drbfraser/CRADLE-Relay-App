package com.cradleplatform.cradle_vsa_sms_relay.repository

import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayResponse
import com.cradleplatform.cradle_vsa_sms_relay.service.SMSRelayService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
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

/**
 * class to make requests to the server
 * class registers callbacks for when the response is received from the server
 * callbacks process the response and initiate the sms protocol to send data to mobile
 */

class HttpsRequestRepository(
    token: String,
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

    suspend fun relayRequestToServer(phoneNumber: String, data: String): retrofit2.Response<HttpRelayResponse> {
        val httpsRequest = HttpRelayRequest(
            phoneNumber = phoneNumber,
            encryptedData = data
        )

        return smsRelayService.postSMSRelay(httpsRequest)
    }

    companion object {
        const val TAG = "HttpsRequestRepository"
    }
}
