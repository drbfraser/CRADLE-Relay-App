package com.cradleplatform.cradle_vsa_sms_relay.model

/// The format of the relay request before we send it to the server
data class HttpRelayRequest(
    val phoneNumber: String,
    val encryptedData: String
)

/// Response from the server for our relay request
data class HttpRelayResponse(
    val code: Int,
    val body: String
)


