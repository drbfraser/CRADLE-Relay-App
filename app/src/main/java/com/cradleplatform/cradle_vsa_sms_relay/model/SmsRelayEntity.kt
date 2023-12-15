package com.cradleplatform.cradle_vsa_sms_relay.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.SmsListConverter
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.TimeStampListConverter
import java.io.Serializable

@Entity
data class SmsRelayEntity(
    @PrimaryKey
    val id: String,

    // to track how many messages have been received
    var numFragmentsReceived: Int,

    // to track how many messages are part of the the entire relay request
    var totalFragmentsFromMobile: Int,

    @TypeConverters(SmsListConverter::class)
    val smsPacketsFromMobile: MutableList<String>,
    val timeRequestInitiated: Long,
    @TypeConverters(TimeStampListConverter::class)
    val timestampsDataMessagesReceived: MutableList<Long>,
    @TypeConverters(TimeStampListConverter::class)
    val timestampsDataMessagesSent: MutableList<Long>,

    //fields for receiving response from server
    var isServerResponseReceived: Boolean,
    var isServerError: Boolean?,
    var errorMessage: String?,
    @TypeConverters(SmsListConverter::class)
    val smsPacketsToMobile: MutableList<String>,
    var numFragmentsSentToMobile: Int?,
    val totalFragmentsFromServer: Int?,

    //extras
    var numberOfTriesUploaded: Int,
    var deliveryReportSent: Boolean,

    var isCompleted: Boolean
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
