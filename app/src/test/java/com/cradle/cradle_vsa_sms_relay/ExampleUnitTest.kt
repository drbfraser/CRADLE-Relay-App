package com.cradleplatform.smsrelay

import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.cradleplatform.cradle_vsa_sms_relay.repository.AuthInterceptor
import com.cradleplatform.cradle_vsa_sms_relay.service.SMSRelayService
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import okhttp3.OkHttpClient
import org.junit.Test

import org.junit.Assert.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)

        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTY4MDk0NDU2OCwianRpIjoiOTgzYzYzOTUtYmVlZC00YWI4LTk1NmItN2ZhNjQyNDAxNGFjIiwidHlwZSI6ImFjY2VzcyIsInN1YiI6eyJlbWFpbCI6ImFkbWluMTIzQGFkbWluLmNvbSIsInJvbGUiOiJBRE1JTiIsImZpcnN0TmFtZSI6IkFkbWluIiwiaGVhbHRoRmFjaWxpdHlOYW1lIjoiSDAwMDAiLCJpc0xvZ2dlZEluIjp0cnVlLCJ1c2VySWQiOjEsInBob25lTnVtYmVyIjoiKzEtMTIzLTQ1Ni03ODkwIiwic3VwZXJ2aXNlcyI6W119LCJuYmYiOjE2ODA5NDQ1NjgsImV4cCI6MTY4MTU0OTM2OH0.RgbaYNdB9_pWuK0xLw8cqfoMgGTdeP-OO_WAB_VAtHU"

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(token))
            .build()
        val service = Retrofit.Builder()
            .baseUrl("http://localhost:5000/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SMSRelayService::class.java)

        println(SMSFormatter.convertSMSHttpRequestToHttpsRequest(SMSHttpRequest(
            "+1-123-456-7890",
            "0000000",
            4,
            mutableListOf(
                "Hello",
                "Myname",
                "Is",
                "Kodai"
            ),
        true
        )))

//        println(service.postSMSRelay(HTTPSRequest("+1-123-456-7890", "Test"))
    }
}
