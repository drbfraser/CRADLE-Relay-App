package com.cradle.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri import android.telephony.SmsMessage
import com.cradle.cradle_vsa_sms_relay.MultiMessageListener
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.utilities.ReferralMessageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * detects messages receives
 */
class MessageReciever(val context: Context) : BroadcastReceiver() {

        private var meListener: MultiMessageListener? = null
        private val LAST_RUN_PREF = "sharedPrefLastTimeServiceRun"
        private val LAST_RUN_TIME = "lastTimeServiceRun"

        fun bindListener(messageListener: MultiMessageListener) {
            meListener = messageListener
            //dont want to block main thread
            MainScope().launch(Dispatchers.IO) {
                meListener?.messageMapRecieved(getUnsentSms())
            }
        }

        fun unbindListener() {
            meListener = null
            //update time last listened to sms
            val sharedPreferences = context.getSharedPreferences(LAST_RUN_PREF,Context.MODE_PRIVATE)
            sharedPreferences.edit().putLong(LAST_RUN_TIME,System.currentTimeMillis()).apply()
        }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<Any>

        // may recieve multiple messages at the same time from different numbers so
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
            val currTime = System.currentTimeMillis()
            smsReferralList.add(
                SmsReferralEntitiy(
                    ReferralMessageUtil.getIdFromMessage(entry.value),
                    ReferralMessageUtil.getReferralJsonFromMessage(entry.value),
                    currTime,
                    false,
                    entry.key,
                    0,
                    "",false
                )
            )
        }

        // send it to the service to send to the server
        meListener?.messageMapRecieved(smsReferralList)

    }


    /**
     * Queries sms depending on the time we were listening for sms
     */
    private fun getUnsentSms(): java.util.ArrayList<SmsReferralEntitiy> {
        val sms = java.util.ArrayList<SmsReferralEntitiy>()
        val smsURI = Uri.parse("content://sms/inbox")
        val columns =
            arrayOf("address", "body", "date")
        //check when we were last listening for the messages
        val sharedPreferences = context.getSharedPreferences(LAST_RUN_PREF,Context.MODE_PRIVATE)
        //if the app is running the first time, we dont want to start sending a large number of text messages
        //for now we ignore all the past messages on login.
        val lastRunTime = sharedPreferences.getLong(LAST_RUN_TIME,System.currentTimeMillis())
        val whereClause = "date >= $lastRunTime"
        val cursor = context.contentResolver.query(smsURI, columns, whereClause, null, null)

        while(cursor != null && cursor.moveToNext()) {

            val body = cursor.getString(cursor.getColumnIndex("body"))
            val addresses = cursor.getString((cursor.getColumnIndex("address")))
            val time = cursor.getString((cursor.getColumnIndex("date"))).toLong()
            val id = ReferralMessageUtil.getIdFromMessage(body)
            sms.add(SmsReferralEntitiy(id,ReferralMessageUtil.getReferralJsonFromMessage(body)
                ,time,false,addresses,0,"",false))
        }
        cursor?.close()
        return sms

    }
}