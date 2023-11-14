package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class SMSSenderEntity(
    @PrimaryKey
    //phonenumber-requestcounter
    val id: String,
    val encryptedData: String?,
    val responseCode: String?,
    val phoneNumber: String?,
    // unix stamp for when the callback was completed
    val timeReceived: Long,
    var numMessagesSent: Int = 0,
    val totalMessages: Int
    ) : Serializable, Comparable<SmsReferralEntity> {

    override fun compareTo(other: SmsReferralEntity): Int {
        return (this.timeReceived - other.timeReceived).toInt()
    }
}
