package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.util.Log
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

class ExpandableListData(private val smsRelayEntity: SmsRelayEntity) {

    val data: HashMap<String, List<Map<String, String>>>
        get() {
            val expandableListDetail = HashMap<String, List<Map<String,String>>>()
            val receiveMobileDetails = ArrayList<Map<String,String>>()
            val sendServerDetails = ArrayList<Map<String,String>>()
            val receiveServerDetails = ArrayList<Map<String,String>>()
            val sendMobileDetails = ArrayList<Map<String,String>>()

            val timestampsDataMessagesReceived = smsRelayEntity?.timestampsDataMessagesReceived
            val timestampsDataMessagesSent = smsRelayEntity?.timestampsDataMessagesSent

            smsRelayEntity.smsPacketsFromMobile?.forEachIndexed{index, msg ->
                receiveMobileDetails.add(mapOf("content" to msg,"timestamp" to getRelativeTime(index,timestampsDataMessagesReceived)))
            }


            sendServerDetails.add(mapOf("sentToServer" to smsRelayEntity?.isSentToServer.toString()))

            receiveServerDetails.add(mapOf("receivedFromServer" to smsRelayEntity?.isServerResponseReceived.toString(), "error message" to smsRelayEntity?.errorMessage.toString() ))


            smsRelayEntity?.smsPacketsToMobile?.forEachIndexed{index, msg ->
                sendMobileDetails.add(mapOf("content" to msg,"timestamp" to getRelativeTime(index,timestampsDataMessagesSent)))
            }

            expandableListDetail["Message received from mobile"] = receiveMobileDetails
            expandableListDetail["Message sent to server"] = sendServerDetails
            expandableListDetail["Response received from server "] = receiveServerDetails
            expandableListDetail["Response sent to mobile "] = sendMobileDetails

            return expandableListDetail
        }

    private fun getRelativeTime(msgPos: Int, timestampsDataMessagesReceived: MutableList<Long>? ): String {
        val format = SimpleDateFormat("HH:mm:ss")
        if(msgPos != 0 && timestampsDataMessagesReceived != null){
            val diff = timestampsDataMessagesReceived[msgPos] - timestampsDataMessagesReceived[msgPos - 1]
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24

            return when {
                hours > 0 -> "$hours hr $minutes min $seconds sec"
                minutes > 0 -> "$minutes min $seconds sec"
                else -> "$seconds sec"
            }
        }
        return format.format(timestampsDataMessagesReceived?.get(msgPos))
    }

}