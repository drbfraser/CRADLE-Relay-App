package com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.cradleplatform.smsrelay.dagger.MyApp
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayRepository
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.cradleplatform.cradle_vsa_sms_relay.repository.HttpsRequestRepository
import com.cradleplatform.cradle_vsa_sms_relay.utilities.ReferralMessageUtil
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import com.cradleplatform.cradle_vsa_sms_relay.view_model.SMSHttpRequestViewModel
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * detects messages receives
 */

// used to get the request number from the message
const val REQUEST_COUNTER_IDX = 2
// index of num fragments after encrypted message is split
const val NUMBER_OF_FRAGMENTS_IDX = 3

class MessageReceiver(private val context: Context) : BroadcastReceiver() {
    private val tag = "MESSAGE_RECEIVER"

    @Inject
    lateinit var repository: ReferralRepository

    @Inject
    lateinit var smsHttpRequestViewModel: SMSHttpRequestViewModel

    @Inject
    lateinit var smsFormatter: SMSFormatter

    @Inject
    lateinit var smsRelayRepository: SmsRelayRepository

    @Inject
    lateinit var httpsRequestRepository: HttpsRequestRepository

    private val smsManager = SmsManager.getDefault()

    private val hash: HashMap<String, String> = hashMapOf()

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

        smsHttpRequestViewModel.smsManager = smsManager

        // may receive multiple messages at the same time from different numbers so
        // we keep track of all the messages from different numbers
        val messages = processSMSMessages(pdus)

        // HashMap to track data messages, ack messages are not saved here
//        val dataMessages = HashMap<String, String>()

        messages.entries.forEach { entry ->

            val message = entry.value
            val phoneNumber = entry.key
            // Exceptions. Escape forEach and show error message.
            if (message.isEmpty()) {
                Log.w(tag, "message is empty")
                Toast.makeText(
                    context,
                    "Warning: received a message that is empty.",
                    Toast.LENGTH_SHORT
                ).show()
                return@forEach
            }
            if (phoneNumber.isEmpty()) {
                Log.w(tag, "phone number is empty")
                Toast.makeText(
                    context,
                    "Warning: received a message without a phone number.",
                    Toast.LENGTH_SHORT
                ).show()
                return@forEach
            }
            // Process SMS
            if (smsFormatter.isAckMessage(message)) {

                val id = "$phoneNumber-${smsFormatter.getAckRequestIdentifier(message)}"
                val smsSenderEntity = smsHttpRequestViewModel.smsSenderTrackerHashMap[id]
                val encryptedPacketList = smsSenderEntity?.encryptedData

                if (encryptedPacketList.isNullOrEmpty()) {
                    smsHttpRequestViewModel.smsSenderTrackerHashMap.remove(id)
                } else {
                    val encryptedPacket = encryptedPacketList.removeAt(0)
                    smsFormatter.sendMessage(phoneNumber, encryptedPacket!!)
                    smsSenderEntity?.numMessagesSent = smsSenderEntity!!.numMessagesSent + 1
                    smsHttpRequestViewModel.smsSenderTrackerHashMap[id] = smsSenderEntity
                }
            } else if (smsFormatter.isFirstMessage(message)) {
                // create new relay entity
                Thread {
                    val requestIdentifier = smsFormatter.getNewRequestIdentifier(message)
                    val id = "${phoneNumber}-${requestIdentifier}"
                    val totalFragments = smsFormatter.getTotalNumOfFragments(message)
                    val currentTime = System.currentTimeMillis()
                    val newRelayEntity = SmsRelayEntity(
                        id,
                        1,
                        totalFragments,
                        smsFormatter.getEncryptedDataFromFirstMessage(message),
                        currentTime,
                        currentTime,
                        null,
                        0,
                        false
                    )
                    smsRelayRepository.insertBlocking(newRelayEntity)

                    smsFormatter.sendAckMessage(newRelayEntity)

                    hash[phoneNumber] = requestIdentifier

                    // add conditional to send http request if all data is received
                }.start()
            } else if (smsFormatter.isRestMessage(message)) {

                Thread {
//                    val requestIdentifier = smsFormatter.getRequestIdentifier(message)
                    val requestIdentifier = hash[phoneNumber]
                    val id = "${phoneNumber}-${requestIdentifier}"
                    val currentTime = System.currentTimeMillis()
                    val relayEntity = smsRelayRepository.getReferralBlocking(id)

                    //update required fields
                    relayEntity!!.timeLastDataMessageReceived = currentTime
                    relayEntity.encryptedDataFromMobile =
                        relayEntity.encryptedDataFromMobile + smsFormatter.getEncryptedDataFromMessage(
                            message
                        )
                    relayEntity.numFragmentsReceived += 1

                    smsRelayRepository.updateBlocking(relayEntity)

                    smsFormatter.sendAckMessage(relayEntity)

                    if (relayEntity.numFragmentsReceived == relayEntity.totalFragmentsFromMobile) {
                        httpsRequestRepository.sendToServer(relayEntity)
                    }
                }.start()
            }
        }
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
