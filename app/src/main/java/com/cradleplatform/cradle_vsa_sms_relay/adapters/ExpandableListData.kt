package com.cradleplatform.cradle_vsa_sms_relay.adapters

import com.cradleplatform.cradle_vsa_sms_relay.activities.MessageDeconstructionConstants
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestPhase
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class ExpandableListData(private val relayRequest: RelayRequest) {

    val data: HashMap<String, List<Map<String, String>>>
        get() {
            val expandableListDetail = HashMap<String, List<Map<String, String>>>()
            val receiveMobileDetails = ArrayList<Map<String, String>>()
            val sendServerDetails = ArrayList<Map<String, String>>()
            val receiveServerDetails = ArrayList<Map<String, String>>()
            val sendMobileDetails = ArrayList<Map<String, String>>()

            val readyPackets = relayRequest.dataPacketsFromMobile.takeWhile { it != null }
            val timestampsDataMessagesReceived = readyPackets.map { it!!.timeMsReceived }

            readyPackets.forEachIndexed { idx, data ->
                val timeKey = if (idx == 0) "Time received" else "Relative time"

                val content = data?.data ?: ""
                receiveMobileDetails.add(
                    mapOf(
                        "Message" to "${idx + 1}/" +
                                "${relayRequest.numPacketsReceived()} ",
                        "Content" to content,
                        timeKey to getRelativeTime(idx, timestampsDataMessagesReceived)
                    )
                )
            }

            if (relayRequest.requestPhase == RelayRequestPhase.RELAYING_TO_MOBILE) {
                receiveServerDetails.add(
                    mapOf(
                        "receivedFromServer" to "true"
                    )
                )
            }

            sendMobileDetails.add(
                mapOf(
                    "Sending Progress" to "${relayRequest.numPacketsSent()}/" +
                            "${relayRequest.dataPacketsToMobile.size} "
                )
            )

            val targetList = when (relayRequest.requestPhase) {
                RelayRequestPhase.RECEIVING_FROM_MOBILE -> receiveMobileDetails
                RelayRequestPhase.RELAYING_TO_SERVER -> receiveServerDetails
                RelayRequestPhase.RECEIVING_FROM_SERVER -> receiveServerDetails
                RelayRequestPhase.RELAYING_TO_MOBILE -> sendMobileDetails
                RelayRequestPhase.COMPLETE -> sendMobileDetails
            }

            if (relayRequest.errorMessage != null) {
                targetList.add(
                    mapOf(
                        "error message" to relayRequest.errorMessage.toString()
                    )
                )
            }

            expandableListDetail["Message received from mobile"] = receiveMobileDetails
            expandableListDetail["Message sent to server"] = sendServerDetails
            expandableListDetail["Response received from server "] = receiveServerDetails
            expandableListDetail["Response sent to mobile "] = sendMobileDetails

            return expandableListDetail
        }

    private fun getRelativeTime(msgPos: Int, timestampList: List<Long>?): String {
        val format = SimpleDateFormat("HH:mm:ss")
        if (msgPos != 0 && timestampList != null) {
            val diff = timestampList[msgPos] - timestampList[msgPos - 1]
            val seconds =
                TimeUnit.MILLISECONDS.toSeconds(diff) % MessageDeconstructionConstants.SECONDS_PER_MINUTE
            val minutes =
                TimeUnit.MILLISECONDS.toMinutes(diff) % MessageDeconstructionConstants.MINUTES_PER_HOUR
            val hours =
                TimeUnit.MILLISECONDS.toHours(diff) % MessageDeconstructionConstants.HOURS_IN_DAY

            return when {
                hours > 0 -> "$hours hr $minutes min $seconds sec"
                minutes > 0 -> "$minutes min $seconds sec"
                else -> "$seconds sec"
            }

        }
        return format.format(timestampList?.get(msgPos))
    }
}
