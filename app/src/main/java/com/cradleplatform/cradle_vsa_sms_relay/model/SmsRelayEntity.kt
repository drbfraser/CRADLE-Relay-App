package com.cradleplatform.cradle_vsa_sms_relay.model

import androidx.room.Entity
import androidx.room.TypeConverters
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.RelayRequestDataListConverter
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.RelayResponseDataListConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class RelayRequestData(
    val data: String,
    val timeMsReceived: Long
)

data class RelayResponseData(
    val data: String,
    var ackedCount: Int,
)

enum class RelayRequestPhase {
    RECEIVING_FROM_MOBILE,
    RELAYING_TO_SERVER,
    RECEIVING_FROM_SERVER,
    RELAYING_TO_MOBILE,
    COMPLETE
}

// RelayRequestResult ideally should be an enum but having the following structure makes querying in
// the database with room much cleaner and easier
object RelayRequestResult {
    const val PENDING = "PENDING" // PENDING is not valid when the relay request phase is COMPLETE
    const val OK = "OK" // OK is only valid when the relay request phase is COMPLETE
    const val ERROR = "ERROR" // ERROR is valid for all phases
}

/// Represents a Relay Request initiated by CRADLE-Mobile
@Entity(
    primaryKeys = ["requestId", "phoneNumber"]
)
data class RelayRequest(
    val requestId: Int,

    // TODO: Validate phone number format -> important when sending sms messages to this number
    // TODO: Make phoneNumber a domain specific type
    val phoneNumber: String,

    val expectedNumPackets: Int,
    var requestPhase: RelayRequestPhase,
    var requestResult: String,

    // The following two time data can be retrieved from dataPacketsFromMobile and dataPacketsToMobile
    // but having it here too is very convenient
    val timeMsInitiated: Long,
    var timeMsLastReceived: Long,

    var errorMessage: String? = null,

    @TypeConverters(RelayRequestDataListConverter::class)
    val dataPacketsFromMobile: MutableList<RelayRequestData?>,

    @TypeConverters(RelayResponseDataListConverter::class)
    val dataPacketsToMobile: MutableList<RelayResponseData>,

    ) {
    fun getDuration(): String =
        if (requestResult == RelayRequestResult.PENDING) {
            "N/A"
        } else {
            timeMsLastReceived
                .minus(timeMsInitiated)
                .milliseconds
                .toComponents { minutes, seconds, _ -> "${minutes}m ${seconds}s" }
        }

    fun getDateAndTime(): String {
        val simpleDateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        val date = Date(timeMsInitiated)
        return simpleDateFormat.format(date)
    }

    fun numPacketsReceived(): Int {
        return dataPacketsFromMobile.filterNotNull().size
    }

    fun numPacketsSent(): Int {
        return dataPacketsToMobile.count { it.isAcked }
    }
}
