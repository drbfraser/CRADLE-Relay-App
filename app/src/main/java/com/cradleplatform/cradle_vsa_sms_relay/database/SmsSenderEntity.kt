package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

data class SmsSenderEntity(
    //phonenumber-requestcounter
    val id: String,
    val encryptedData: MutableList<String>?,
    val responseCode: String?,
    val phoneNumber: String?,
    // unix stamp for when the callback was completed
    val timeReceived: Long,
    val totalMessages: Int,
    var numMessagesSent: Int
    ) : Serializable {
}
