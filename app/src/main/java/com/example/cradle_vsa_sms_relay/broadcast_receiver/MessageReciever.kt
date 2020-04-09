package com.example.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.example.cradle_vsa_sms_relay.MultiMessageListener
import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import org.json.JSONObject

/**
 * detects messages receives
 */
class MessageReciever : BroadcastReceiver() {

    companion object {
        private var meListener: MultiMessageListener? = null

        fun bindListener(messageListener: MultiMessageListener) {
            meListener = messageListener
        }

        fun unbindListener() {
            meListener = null
        }
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<Any>

        // you may recieve multiple messages at the same time from different numbers so
        // we keep track of all the messages from different numbers
        val messages = HashMap<String?, String?>()


        for (element in pdus) {
            // if smsMessage is null, we continue to the next one
            val smsMessage = SmsMessage.createFromPdu(element as ByteArray?) ?: continue
            //one message has length of 153 chars, 7 other chars for user data header


            //We are assuming that no one phone can send multiple long messages at ones.
            // since there is some user delay in either typing or copy/pasting the message
            //or typing 1 char at  a time
            if (messages.containsKey(smsMessage.originatingAddress)) {
                //concatenating messages
                val newMsg: String = smsMessage.messageBody
                val oldMsg: String? = messages[smsMessage.originatingAddress]
                messages[smsMessage.originatingAddress] = oldMsg + newMsg
            } else {
                messages[smsMessage.originatingAddress] = smsMessage.messageBody
            }

        }
        val smsReferralList: ArrayList<SmsReferralEntitiy> = ArrayList()

        messages.entries.forEach { entry ->
            val currTime = System.currentTimeMillis() / 100L
            smsReferralList.add(
                SmsReferralEntitiy(
                    getIdFromMessage(entry.value),
                    getReferralJsonFromMessage(entry.value),
                    currTime,
                    false,
                    entry.key,
                    0,
                    ""
                )
            )
        }
        // send it to the service to send to the server
        meListener?.messageMapRecieved(smsReferralList)

    }

    private val referralJsonKeys = mapOf("0" to "patient",
                                "1" to "patientId",
                                "2" to "patientName",
                                "3" to "dob",
                                "4" to "patientAge",
                                "5" to "gestationalAgeUnit",
                                "6" to "gestationalAgeValue",
                                "7" to "villageNumber",
                                "8" to "patientSex",
                                "9" to "zone",
                                "10" to "isPregnant",
                                "11" to "reading",
                                "12" to "readingId",
                                "13" to "dateLastSaved",
                                "14" to "dateTimeTaken",
                                "15" to "bpSystolic",
                                "16" to "urineTests",
                                "17" to "urineTestBlood",
                                "18" to "urineTestPro",
                                "19" to "urineTestLeuc",
                                "20" to "urineTestGlu",
                                "21" to "urineTestNit",
                                "22" to "userId",
                                "23" to "bpDiastolic",
                                "24" to "heartRateBPM",
                                "25" to "dateRecheckVitalsNeeded",
                                "26" to "isFlaggedForFollowup",
                                "27" to "symptoms",
                                "28" to "comment",
                                "29" to "healthFacilityName",
                                "30" to "date",
                                "31" to "referralId")

    private fun getReferralJsonFromMessage(message: String?): String {
        val referralJsonObject = JSONObject(message)
        // remove referralId
        referralJsonObject.remove("31")

        // replace number keys with string keys
        var newMessage = referralJsonObject.toString()
        for (key in referralJsonKeys.keys) {
            newMessage = newMessage.replace("\"$key\":", "\"" + referralJsonKeys[key] + "\":")
        }
        return newMessage
    }

    private fun getIdFromMessage(message: String?): String {
        val jsonObject = JSONObject(message)
        return jsonObject.getString("31")
    }
}