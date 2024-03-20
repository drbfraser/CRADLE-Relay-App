package com.cradleplatform.cradle_vsa_sms_relay.service

import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SMSRelayService {
    @POST("api/sms_relay")
    fun postSMSRelay(@Body body: HTTPSRequest): Call<HTTPSResponse>
}
