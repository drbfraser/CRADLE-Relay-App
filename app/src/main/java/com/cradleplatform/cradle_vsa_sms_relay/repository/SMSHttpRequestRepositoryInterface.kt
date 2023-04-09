package com.cradleplatform.cradle_vsa_sms_relay.repository

import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import retrofit2.Call

interface SMSHttpRequestRepositoryInterface {
    fun sendSMSHttpRequestToServer(smsHttpRequest: SMSHttpRequest): Call<HTTPSResponse>

}