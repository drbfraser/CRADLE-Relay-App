package com.cradleplatform.cradle_vsa_sms_relay.service

import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SMSRelayService {
    @POST("api/sms_relay")
    suspend fun postSMSRelay(@Body body: HttpRelayRequest): Response<HttpRelayResponse>
}
