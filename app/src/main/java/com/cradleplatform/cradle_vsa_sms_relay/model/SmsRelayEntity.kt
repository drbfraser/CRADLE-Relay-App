package com.cradleplatform.cradle_vsa_sms_relay.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.SmsListConverter
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.TimeStampListConverter
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * data class used to store the status of a single SMS Relay transaction
 * the class also stores all information pertaining to a single transaction
 */

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
    val timeRequestInitiated: Long = System.currentTimeMillis(),
    @TypeConverters(TimeStampListConverter::class)
    val timestampsDataMessagesReceived: MutableList<Long>,
    @TypeConverters(TimeStampListConverter::class)
    val timestampsDataMessagesSent: MutableList<Long>,

    // fields for receiving response from server
    var isServerResponseReceived: Boolean,
    var isServerError: Boolean?,
    var errorMessage: String?,
    @TypeConverters(SmsListConverter::class)
    val smsPacketsToMobile: MutableList<String>,
    var numFragmentsSentToMobile: Int?,
    val totalFragmentsFromServer: Int?,

    // extras
    var numberOfTriesUploaded: Int,
    var deliveryReportSent: Boolean,
    var isCompleted: Boolean
) : Serializable, Comparable<SmsRelayEntity> {

    override fun compareTo(other: SmsRelayEntity): Int {
        return (this.timeRequestInitiated - other.timeRequestInitiated).toInt()
    }

    fun getPhoneNumber(): String {
        return this.id.split("-")[0]
    }

    fun getRequestIdentifier(): String {
        return this.id.split("-")[1]
    }
    fun getDateAndTime(): String {
        val simpleDateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        val date = Date(timeRequestInitiated)
        return simpleDateFormat.format(date)
    }
    fun getDuration(): String {
        val receivedTime = if (timestampsDataMessagesReceived.isNotEmpty()) timestampsDataMessagesReceived[0] else 0
        val sentTime = if (timestampsDataMessagesSent.isNotEmpty()) timestampsDataMessagesSent[0] else 0

        val durationInSeconds = (sentTime - receivedTime) / 1000

        val minutes = durationInSeconds / SIXTY
        val seconds = durationInSeconds % SIXTY

        return String.format("%dm %ds", minutes, seconds)

    }

    companion object {
        private const val SIXTY = 60
    }

}
