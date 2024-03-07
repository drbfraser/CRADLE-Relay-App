package com.cradleplatform.sms_relay.model

data class HTTPSRequest(
    val phoneNumber: String,
    val encryptedData: String
)
