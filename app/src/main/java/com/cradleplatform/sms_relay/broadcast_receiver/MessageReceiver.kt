package com.cradleplatform.sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import com.cradleplatform.sms_relay.dagger.MyApp
import com.cradleplatform.sms_relay.model.SmsRelayEntity
import com.cradleplatform.sms_relay.repository.SmsRelayRepository
import com.cradleplatform.sms_relay.repository.HttpsRequestRepository
import com.cradleplatform.sms_relay.utilities.SMSFormatter
import javax.inject.Inject
import kotlin.collections.HashMap

/**
 * detects and processes received messages
 */

class MessageReceiver(private val context: Context) : BroadcastReceiver() {
    private val tag = "MESSAGE_RECEIVER"

    @Inject
    lateinit var smsFormatter: SMSFormatter

    @Inject
    lateinit var smsRelayRepository: SmsRelayRepository

    @Inject
    lateinit var httpsRequestRepository: HttpsRequestRepository

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

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(tag, "Message Received")
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<*>

        // may receive multiple messages at the same time from different numbers so
        // we keep track of all the messages from different numbers
        val messages = processSMSMessages(pdus)

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

                val requestIdentifier = smsFormatter.getAckRequestIdentifier(message)
                val id = "$phoneNumber-$requestIdentifier"

                val relayEntity = smsRelayRepository.getRelayBlocking(id)

                // ignore ACK message if there are no more packets to send
                if (relayEntity!!.smsPacketsToMobile.isNotEmpty()) {
                    val encryptedPacket = relayEntity.smsPacketsToMobile.removeAt(0)
                    relayEntity.numFragmentsSentToMobile = relayEntity.numFragmentsSentToMobile!! + 1
                    smsRelayRepository.updateBlocking(relayEntity)
                    smsFormatter.sendMessage(phoneNumber, encryptedPacket)
                } else {
                    relayEntity.isCompleted = true
                    smsRelayRepository.update(relayEntity)
                }
            } else if (smsFormatter.isFirstMessage(message)) {
                // create new relay entity
                Thread {
                    val requestIdentifier = smsFormatter.getNewRequestIdentifier(message)
                    val id = "$phoneNumber-$requestIdentifier"
                    val totalFragments = smsFormatter.getTotalNumOfFragments(message)
                    val currentTime = System.currentTimeMillis()
                    val newRelayEntity = SmsRelayEntity(
                        id,
                        1,
                        totalFragments,
                        mutableListOf(message),
                        currentTime,
                        mutableListOf(currentTime),
                        mutableListOf(),
                        false,
                        false,
                        null,
                        mutableListOf(),
                        null,
                        null,
                        0,
                        false,
                        false
                    )

                    if (newRelayEntity.numFragmentsReceived == newRelayEntity.totalFragmentsFromMobile) {
                        newRelayEntity.numberOfTriesUploaded = 1
                    }

                    smsRelayRepository.insertBlocking(newRelayEntity)

                    smsFormatter.sendAckMessage(newRelayEntity)

                    hash[phoneNumber] = requestIdentifier

                    if (newRelayEntity.numFragmentsReceived == newRelayEntity.totalFragmentsFromMobile) {
                        httpsRequestRepository.sendToServer(newRelayEntity)
                    }
                }.start()
            } else if (smsFormatter.isRestMessage(message)) {

                Thread {
                    val requestIdentifier = hash[phoneNumber]
                    val id = "$phoneNumber-$requestIdentifier"
                    val currentTime = System.currentTimeMillis()
                    val relayEntity = smsRelayRepository.getRelayBlocking(id)

                    // update required fields
                    relayEntity!!.timestampsDataMessagesReceived.add(currentTime)
                    relayEntity.smsPacketsFromMobile.add(message)
                    relayEntity.numFragmentsReceived += 1

                    if (relayEntity.numFragmentsReceived == relayEntity.totalFragmentsFromMobile) {
                        relayEntity.numberOfTriesUploaded = 1
                    }

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

    companion object {
        private const val LAST_RUN_PREF = "sharedPrefLastTimeServiceRun"
        private const val LAST_RUN_TIME = "lastTimeServiceRun"
    }
}
