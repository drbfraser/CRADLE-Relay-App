package com.cradleplatform.cradle_vsa_sms_relay.model

import kotlinx.serialization.Serializable

// The format of the relay request before we send it to the server.
@Serializable
data class HttpRelayRequestBody(
    val phoneNumber: String,
    val encryptedData: String
)

// Response from the server for our relay request
@Serializable
data class HttpRelayResponseBody(
    val code: Int,
    val body: String
)


