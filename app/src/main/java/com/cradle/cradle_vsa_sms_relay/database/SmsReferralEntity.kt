package com.cradle.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class SmsReferralEntity(
    @PrimaryKey
    val id: String,
    val jsonData: String?,
    //unix stamp
    val timeReceived: Long,
    var isUploaded: Boolean,
    val phoneNumber: String?,
    var numberOfTriesUploaded: Int,
    var errorMessage:String,
    var deliveryReportSent:Boolean
) : Serializable, Comparable<SmsReferralEntity> {


    override fun compareTo(other: SmsReferralEntity): Int {
        return (this.timeReceived - other.timeReceived).toInt()
    }

}