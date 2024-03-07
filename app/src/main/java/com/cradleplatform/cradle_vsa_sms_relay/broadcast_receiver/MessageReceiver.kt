package com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import com.cradleplatform.cradle_vsa_sms_relay.repository.HttpsRequestRepository
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import kotlinx.coroutines.Runnable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

    // TODO: Maybe use a thread-safe TTL cache
    private val hash: ConcurrentHashMap<String, Pair<String, Long>> = ConcurrentHashMap()

    private val scheduler = Executors.newScheduledThreadPool(1)

    init {
        (context.applicationContext as MyApp).component.inject(this)
        scheduler.schedule(startExpirationCheck(MAX_INTERVAL_MS), 0, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        Log.d(tag, "Shutting down scheduled thread pool")
        scheduler.shutdownNow()
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

        var shouldContinue = true
        messages.entries.forEach { entry ->
            if (!shouldContinue) return@forEach

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
                    relayEntity.numFragmentsSentToMobile =
                        relayEntity.numFragmentsSentToMobile!! + 1
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

                    hash[phoneNumber] = Pair(requestIdentifier, System.currentTimeMillis())

                    if (newRelayEntity.numFragmentsReceived == newRelayEntity.totalFragmentsFromMobile) {
                        httpsRequestRepository.sendToServer(newRelayEntity)
                    }
                }.start()
            } else if (smsFormatter.isRestMessage(message)) {
                Thread {
                    // Exit early because this hash key expired
                    if (!hash.containsKey(phoneNumber)) {
                        shouldContinue = false
                        return@Thread
                    }
                    // Passive expiry check
                    else if (isKeyExpired(phoneNumber)) {
                        Log.d(tag, "$phoneNumber has expired, evicting it from the hash")
                        hash.remove(phoneNumber)
                        shouldContinue = false
                        return@Thread
                    }

                    val requestIdentifier = hash[phoneNumber]!!.first
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

                    // Update last received timestamp
                    hash[phoneNumber] =
                        hash[phoneNumber]!!.copy(second = System.currentTimeMillis())
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

    private fun startExpirationCheck(interval: Long): Runnable {
        return Runnable {
            val startExe = System.currentTimeMillis()
            try {
                Log.d(tag, "Actively checking for expired keys")

                val hashSize = hash.size
                hash.forEach { (key, _) ->
                    if (isKeyExpired(key, startExe)) {
                        Log.d(tag, "$key has expired, evicting it from the hash")
                        hash.remove(key)
                    }
                }

                // Interval is dynamic to ensure sufficient clean up of resources
                val newInterval = when {
                    hashSize >= 100 && hash.size < hashSize * (1 - SIZE_PERCENT_THRESHOLD) -> max(
                        MIN_INTERVAL_MS,
                        interval / 2
                    )

                    else -> min(MAX_INTERVAL_MS, interval * 2)
                }

                Log.d(tag, "Previous interval: $interval; new interval: $newInterval")

                scheduler.schedule(
                    startExpirationCheck(newInterval),
                    abs(newInterval - (System.currentTimeMillis() - startExe)),
                    TimeUnit.MILLISECONDS
                )
            } catch (e: Exception) {
                Log.e(tag, "Exception occurred in startExpirationCheck", e)
                scheduler.schedule(
                    startExpirationCheck(interval),
                    // TODO: It may be useful to reduce the interval in half due to failure
                    abs(interval - (System.currentTimeMillis() - startExe)),
                    TimeUnit.MILLISECONDS
                )
            }
        }
    }

    private fun isKeyExpired(key: String, currentTime: Long = System.currentTimeMillis()): Boolean {
        val timestamp = hash[key]!!.second
        return currentTime - timestamp > TIMEOUT_SECONDS * 1000
    }

    companion object {
        private const val LAST_RUN_PREF = "sharedPrefLastTimeServiceRun"
        private const val LAST_RUN_TIME = "lastTimeServiceRun"
        private const val TIMEOUT_SECONDS = 200
        private const val SIZE_PERCENT_THRESHOLD = 0.25
        private const val MIN_INTERVAL_MS: Long = 1000
        private const val MAX_INTERVAL_MS: Long = 32000
    }
}
