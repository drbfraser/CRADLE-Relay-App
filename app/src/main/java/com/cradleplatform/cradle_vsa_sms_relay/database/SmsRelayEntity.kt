package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class SmsRelayEntity(
    @PrimaryKey
    val id: String,
    // to track how many messages have been received
    var numFragmentsReceived: Int,
    // to track how many messages are part of the the entire relay request
    val totalFragmentsFromMobile: Int,
    // data received from mobile as a single string
    // string is appended to when more data is received
    var encryptedDataFromMobile: String,
    val timeRequestInitiated: Long,
    var timeLastDataMessageReceived: Long,
    val timeLastDataMessageSent: Long?,
    var numberOfTriesUploaded: Int,
    var deliveryReportSent: Boolean

//    //encrypted data or unencrypted message
//    val dataFromServer: String?,
//    val smsPackets: ArrayList<String>,
//    val totalFragmentsFromServer: Int,
//    val numFragmentsSentToMobile: Int,
) : Serializable, Comparable<SmsRelayEntity> {

    override fun compareTo(other: SmsRelayEntity): Int {
        return (this.timeRequestInitiated - other.timeRequestInitiated).toInt()
    }

    fun getPhoneNumber(): String{
        return this.id.split("-")[0]
    }

    fun getRequestIdentifier(): String{
        return this.id.split("-")[1]
    }
}
