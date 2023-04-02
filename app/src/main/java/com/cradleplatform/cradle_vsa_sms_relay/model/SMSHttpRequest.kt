package com.cradleplatform.cradle_vsa_sms_relay.model

data class SMSHttpRequest(
    val phoneNumber: String,
    val requestCounter: String,
    val numOfFragments: Int,
    val encryptedFragments: MutableList<String>,
    var isReadyToSendToServer: Boolean
)