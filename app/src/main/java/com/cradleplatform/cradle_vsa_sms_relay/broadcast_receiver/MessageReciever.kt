package com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Base64
import android.util.Log
import com.cradleplatform.smsrelay.dagger.MyApp
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.cradleplatform.cradle_vsa_sms_relay.utilities.ReferralMessageUtil
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import com.cradleplatform.cradle_vsa_sms_relay.view_model.SMSHttpRequestViewModel
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * detects messages receives
 */

//
const val REQUEST_COUNTER_IDX = 2
// index of num fragments after encrypted message is split
const val NUMBER_OF_FRAGMENTS_IDX = 3

class MessageReciever(private val context: Context) : BroadcastReceiver() {
    private val tag = "MESSAGE_RECEIVER"

    @Inject
    lateinit var repository: ReferralRepository

    @Inject
    lateinit var smsHttpRequestViewModel: SMSHttpRequestViewModel

    private val smsManager = SmsManager.getDefault()

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

    private fun saveSMSReferralEntity(messages: HashMap<String, String>) {
        val smsReferralList: ArrayList<SmsReferralEntity> = ArrayList()

        messages.entries.forEach { entry ->
            val currTime = System.currentTimeMillis()
            val phoneNumber = entry.key
            val encryptedData = entry.value
            val requestCounter = smsHttpRequestViewModel.phoneNumberToRequestCounter[phoneNumber]!!.requestCounter
            val fragmentIdx = String.format(
                "%03d",
                smsHttpRequestViewModel.phoneNumberToRequestCounter[phoneNumber]!!.encryptedFragments.size - 1
            )
            smsReferralList.add(
                SmsReferralEntity(
                    "$phoneNumber-$requestCounter-$fragmentIdx",
                    encryptedData,
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

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(tag, "Message Received")
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<*>

        // may recieve multiple messages at the same time from different numbers so
        // we keep track of all the messages from different numbers
        val messages = processSMSMessages(pdus)

        messages.entries.forEach { entry ->
            if (entry.key.isNotEmpty() && entry.value.isNotEmpty()) {
                val smsHttpRequest = createSMSHttpRequest(entry.key, entry.value)
                sendAcknowledgementMessage(smsHttpRequest)

                if (smsHttpRequest.isReadyToSendToServer) {
                    smsHttpRequestViewModel.referralRepository = repository
                    smsHttpRequestViewModel.sendSMSHttpRequestToServer(smsHttpRequest)
                }
            }
        }

        saveSMSReferralEntity(messages)
    }

    private fun processSMSMessages(pdus: Array<*>): HashMap<String, String> {
        val messages = HashMap<String, String>()
        for (element in pdus) {
            // if smsMessage is null, we continue to the next one
            val smsMessage = SmsMessage.createFromPdu(element as ByteArray?) ?: continue
            // one message has length of 153 chars, 7 other chars for user data header

            val originatingPhoneNumber = smsMessage.originatingAddress ?: continue

            // We are assuming that no one phone can send multiple long messages at ones.
            // since there is some user delay in either typing or copy/pasting the message
            // or typing 1 char at  a time
            if (messages.containsKey(originatingPhoneNumber)) {
                // concatenating messages
                val newMsg: String = smsMessage.messageBody
                val oldMsg: String = messages[originatingPhoneNumber]!!
                messages[originatingPhoneNumber] = oldMsg + newMsg
            } else {
                messages[originatingPhoneNumber] = smsMessage.messageBody
            }
        }
        return messages
    }

    private fun createSMSHttpRequest(phoneNumber: String, encryptedValue: String): SMSHttpRequest {
        val isFirstFragment = SMSFormatter.isSMSPacketFirstFragment(encryptedValue)
        val packetComponents = SMSFormatter.decomposeSMSPacket(encryptedValue, isFirstFragment)

        return if (isFirstFragment) {
            val requestCounter = packetComponents[REQUEST_COUNTER_IDX]
            val numberOfFragments = packetComponents[NUMBER_OF_FRAGMENTS_IDX].toInt()
            SMSHttpRequest(
                phoneNumber,
                requestCounter,
                numberOfFragments,
                // list of encrypted messages
                mutableListOf(packetComponents.last()),
                false
            )
        } else {
            val fragmentNumber = packetComponents.first()
            val smsHttpRequest = smsHttpRequestViewModel.phoneNumberToRequestCounter[phoneNumber]!!
            smsHttpRequest.encryptedFragments.add(packetComponents.last())

            if (smsHttpRequest.numOfFragments == fragmentNumber.toInt() + 1) {
                smsHttpRequest.isReadyToSendToServer = true
            }

            smsHttpRequest
        }
    }

    private fun sendAcknowledgementMessage(smsHttpRequest: SMSHttpRequest) {
        val phoneNumber = smsHttpRequest.phoneNumber
        val ackFragmentNumber: String
        val isFirstFragment = smsHttpRequest.encryptedFragments.size == 1

        if (isFirstFragment) {
            smsHttpRequestViewModel.phoneNumberToRequestCounter[phoneNumber] = smsHttpRequest
            ackFragmentNumber = "000"
        } else {
            ackFragmentNumber = String.format(
                "%03d",
                smsHttpRequestViewModel.phoneNumberToRequestCounter[phoneNumber]!!.encryptedFragments.size - 1
            )
        }

        val ackMessage: String = """
        01
        CRADLE
        ${smsHttpRequest.requestCounter}
        $ackFragmentNumber
        ACK""".trimIndent().replace("\n", "-")

        smsManager.sendMultipartTextMessage(
            phoneNumber, null,
            smsManager.divideMessage(ackMessage),
            null, null
        )
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
