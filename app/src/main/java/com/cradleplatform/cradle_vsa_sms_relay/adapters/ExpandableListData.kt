package com.cradleplatform.cradle_vsa_sms_relay.adapters

import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class ExpandableListData(private val smsRelayEntity: SmsRelayEntity) {

    val data: HashMap<String, List<Map<String, String>>>
        get() {
            val expandableListDetail = HashMap<String, List<Map<String,String>>>()
            val receiveMobileDetails = ArrayList<Map<String,String>>()
            val sendServerDetails = ArrayList<Map<String,String>>()
            val receiveServerDetails = ArrayList<Map<String,String>>()
            val sendMobileDetails = ArrayList<Map<String,String>>()

            val timestampsDataMessagesReceived = smsRelayEntity.timestampsDataMessagesReceived
            val timestampsDataMessagesSent = smsRelayEntity.timestampsDataMessagesSent

            smsRelayEntity.smsPacketsFromMobile?.forEachIndexed{idx, msg ->
                val timeKey = if (idx == 0) "Time received" else "Relative time"
                if(idx == 0) {
                    val content = msg.split("-")[4]
                    receiveMobileDetails.add(mapOf("Content" to content,timeKey to
                            getRelativeTime(idx,timestampsDataMessagesReceived)))
                }
                else {
                    val content = msg.split("-")[1]
                    receiveMobileDetails.add(mapOf("Message" to "${idx}/$" +
                            "{smsRelayEntity.totalFragmentsFromMobile}","Content" to content,
                        timeKey to getRelativeTime(idx,timestampsDataMessagesReceived)))
                }

            }

            sendServerDetails.add(mapOf("sentToServer" to smsRelayEntity.isSentToServer.toString(),
                "retries" to smsRelayEntity.numberOfTriesUploaded.toString()))

            if(smsRelayEntity.isServerError == true){
                receiveServerDetails.add(mapOf(
                    "receivedFromServer" to smsRelayEntity.isServerResponseReceived.toString(),
                    "error message" to smsRelayEntity?.errorMessage.toString() ))
            }
            else {
                if(smsRelayEntity.isKeyExpired){
                    receiveServerDetails.add(mapOf(
                        "receivedFromServer" to smsRelayEntity.isServerResponseReceived.toString(),
                        "isKeyExpired" to smsRelayEntity?.isKeyExpired.toString() ))
                }
                else {
                    receiveServerDetails.add(mapOf(
                        "receivedFromServer" to smsRelayEntity.isServerResponseReceived.toString()))
                }
            }

            timestampsDataMessagesSent?.forEachIndexed{idx, _ ->
                val timeKey = if (idx == 0) "time sent" else "relative time"
                sendMobileDetails.add(mapOf(
                    timeKey to getRelativeTime(idx,timestampsDataMessagesSent)))
            }

            expandableListDetail["Message received from mobile"] = receiveMobileDetails
            expandableListDetail["Message sent to server"] = sendServerDetails
            expandableListDetail["Response received from server "] = receiveServerDetails
            expandableListDetail["Response sent to mobile "] = sendMobileDetails

            return expandableListDetail
        }

    private fun getRelativeTime(msgPos: Int, timestampList: MutableList<Long>? ): String {
        val format = SimpleDateFormat("HH:mm:ss")
        if(msgPos != 0 && timestampList != null){
            val diff = timestampList[msgPos] - timestampList[msgPos - 1]
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24

            return when {
                hours > 0 -> "$hours hr $minutes min $seconds sec"
                minutes > 0 -> "$minutes min $seconds sec"
                else -> "$seconds sec"
            }
        }
        return format.format(timestampList?.get(msgPos))
    }
}
