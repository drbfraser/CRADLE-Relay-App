package com.cradleplatform.cradle_vsa_sms_relay.database

import java.io.Serializable

data class SmsSenderEntity(
    // phonenumber-requestcounter
    val id: String,
    val encryptedData: MutableList<String>?,
    val responseCode: String?, // TODO: Consider making enum class for this
    val phoneNumber: String?,  // TODO: Consider making a PhoneNumber class for unified format
    // unix stamp for when the callback was completed
    val timeReceived: Long,
    val totalMessages: Int,
    var numMessagesSent: Int
) : Serializable
