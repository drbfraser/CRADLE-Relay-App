package com.cradle.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

@Entity
data class SmsReferralEntitiy(
    @PrimaryKey
    val id: String,
    val jsonData: String?,
    //unix stamp
    val timeRecieved: Long,
    var isUploaded: Boolean,
    val phoneNumber: String?,
    var numberOfTriesUploaded: Int,
    var errorMessage:String,
    var deliveryReportSent:Boolean
) : Serializable, Comparable<SmsReferralEntitiy> {


    override fun compareTo(other: SmsReferralEntitiy): Int {
        return (this.timeRecieved - other.timeRecieved).toInt()
    }
}