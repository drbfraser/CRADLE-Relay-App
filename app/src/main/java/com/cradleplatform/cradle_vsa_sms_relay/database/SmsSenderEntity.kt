package com.cradleplatform.cradle_vsa_sms_relay.database

import java.io.Serializable

data class SmsSenderEntity(
    // phonenumber-requestcounter
    val id: String,
    val encryptedData: MutableList<String>?,
    // Consider making enum class for responseCode
    val responseCode: String?,
    // Consider making a PhoneNumber class for a unified format
    val phoneNumber: String?,
    // unix stamp for when the callback was completed
    val timeReceived: Long,
    val totalMessages: Int,
    var numMessagesSent: Int
) : Serializable
