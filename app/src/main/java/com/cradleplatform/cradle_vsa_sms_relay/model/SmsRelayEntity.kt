package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.io.Serializable

@Entity
data class SmsRelayEntity(
    @PrimaryKey
    val id: String,

    // to track how many messages have been received
    var numFragmentsReceived: Int,

    // to track how many messages are part of the the entire relay request
    var totalFragmentsFromMobile: Int,

    // data received from mobile as a single string
    // string is appended to when more data is received
    var encryptedDataFromMobile: String,
    val timeRequestInitiated: Long,
    var timeLastDataMessageReceived: Long,
    var timeLastDataMessageSent: Long?,

    //fields for receiving response from server
    var isServerResponseReceived: Boolean,
    var isServerError: Boolean?,
    var errorMessage: String?,
    @TypeConverters(SmsListConverter::class)
    var smsPackets: MutableList<String>,
    var numFragmentsSentToMobile: Int?,
    val totalFragmentsFromServer: Int?,

    //extras
    var numberOfTriesUploaded: Int,
    var deliveryReportSent: Boolean
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
