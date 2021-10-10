package com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsMessage
import android.util.Base64
import android.util.Log
import com.cradleplatform.smsrelay.dagger.MyApp
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradleplatform.cradle_vsa_sms_relay.utilities.ReferralMessageUtil
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * detects messages receives
 */
class MessageReciever(private val context: Context) : BroadcastReceiver() {
    private val tag = "MESSAGE_RECEIVER"

    @Inject
    lateinit var repository: ReferralRepository

    init {
        (context.applicationContext as MyApp).component.inject(this)
    }

    fun updateLastRunPref() {
        // update time last listened to sms
        val sharedPreferences = context.getSharedPreferences(LAST_RUN_PREF, Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong(LAST_RUN_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun decodeSMSMessage(message: String): String {
        var tempMessage = ""
        val p: Pattern = Pattern.compile("[A-Za-z\\d/+=\n]+")
        val m: Matcher = p.matcher(message)

        while (m.find()) {
            m.group(0)?.let {
                if (it.length > tempMessage.length)
                    tempMessage = it
            }
        }

        // remove the newline characters
        if (tempMessage.isNotEmpty() && tempMessage[0] == '\n')
            tempMessage = tempMessage.substring(1, tempMessage.length - 1)

        tempMessage = try {
            String(Base64.decode(tempMessage, Base64.DEFAULT))
        } catch (e: IllegalArgumentException) {
            message
        }

        return tempMessage
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(tag, "Message Received")
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<*>

        // may recieve multiple messages at the same time from different numbers so
        // we keep track of all the messages from different numbers
        val messages = HashMap<String?, String?>()

        for (element in pdus) {
            // if smsMessage is null, we continue to the next one
            val smsMessage = SmsMessage.createFromPdu(element as ByteArray?) ?: continue
            // one message has length of 153 chars, 7 other chars for user data header

            // We are assuming that no one phone can send multiple long messages at ones.
            // since there is some user delay in either typing or copy/pasting the message
            // or typing 1 char at  a time
            if (messages.containsKey(smsMessage.originatingAddress)) {
                // concatenating messages
                val newMsg: String = smsMessage.messageBody
                val oldMsg: String? = messages[smsMessage.originatingAddress]
                messages[smsMessage.originatingAddress] = oldMsg + newMsg
            } else {
                messages[smsMessage.originatingAddress] = smsMessage.messageBody
            }
        }
        val smsReferralList: ArrayList<SmsReferralEntity> = ArrayList()

        messages.entries.forEach { entry ->
            val currTime = System.currentTimeMillis()
            smsReferralList.add(
                SmsReferralEntity(
                    ReferralMessageUtil.getIdFromMessage(decodeSMSMessage(entry.value.toString())),
                    ReferralMessageUtil.getReferralJsonFromMessage(decodeSMSMessage(entry.value.toString())),
                    currTime,
                    false,
                    entry.key,
                    0,
                    "", false
                )
            )
        }
        repository.insertAll(smsReferralList)
    }

    /**
     * Queries sms depending on the time we were listening for sms
     * //todo need to check for permission
     */
    fun getUnsentSms() {
        GlobalScope.launch(Dispatchers.IO) {

            val sms = ArrayList<SmsReferralEntity>()
            val smsURI = Uri.parse("content://sms/inbox")
            val columns =
                arrayOf("address", "body", "date")
            // check when we were last listening for the messages
            val sharedPreferences =
                context.getSharedPreferences(LAST_RUN_PREF, Context.MODE_PRIVATE)
            // if the app is running the first time, we dont want to start sending a large number of text messages
            // for now we ignore all the past messages on login.
            val lastRunTime =
                sharedPreferences.getLong(LAST_RUN_TIME, System.currentTimeMillis())
            val whereClause = "date >= $lastRunTime"
            val cursor = context.contentResolver.query(smsURI, columns, whereClause, null, null)

            while (cursor != null && cursor.moveToNext()) {

                val body = decodeSMSMessage(cursor.getString(cursor.getColumnIndex("body")))
                val addresses = cursor.getString((cursor.getColumnIndex("address")))
                val time = cursor.getString((cursor.getColumnIndex("date"))).toLong()
                val id = ReferralMessageUtil.getIdFromMessage(body)
                sms.add(
                    SmsReferralEntity(
                        id, ReferralMessageUtil.getReferralJsonFromMessage(body),
                        time, false, addresses, 0, "", false
                    )
                )
            }
            cursor?.close()
            repository.insertAll(sms)
        }
    }

    companion object {
        private const val LAST_RUN_PREF = "sharedPrefLastTimeServiceRun"
        private const val LAST_RUN_TIME = "lastTimeServiceRun"
    }
}
