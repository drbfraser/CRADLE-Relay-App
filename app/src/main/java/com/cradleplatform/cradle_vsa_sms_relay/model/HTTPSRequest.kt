package com.cradleplatform.cradle_vsa_sms_relay.model

data class HTTPSRequest(
    val phoneNumber: String,
    val encryptedData: String
)
