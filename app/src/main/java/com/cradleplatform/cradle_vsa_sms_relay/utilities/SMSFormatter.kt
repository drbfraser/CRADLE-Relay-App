package com.cradleplatform.cradle_vsa_sms_relay.utilities

import android.telephony.SmsManager
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.google.firebase.crashlytics.internal.model.ImmutableList
import kotlin.math.min

private const val NUM_OF_FIRST_PACKET_COMPONENTS = 5
private const val NUM_OF_NON_FIRST_PACKET_COMPONENTS = 2

private const val PACKET_SIZE = 153 * 2
private const val SMS_TUNNEL_PROTOCOL_VERSION = "01"
private const val SMS_ACK_SUFFIX = "ACK"
private const val MAGIC_STRING = "CRADLE"
private const val FRAGMENT_HEADER_LENGTH = 3
private const val REQUEST_NUMBER_LENGTH = 6

private val ackRegexPattern = Regex("^$SMS_TUNNEL_PROTOCOL_VERSION-$MAGIC_STRING-(\\d{6})-(\\d{3})-ACK$")
private val firstRegexPattern = Regex("^$SMS_TUNNEL_PROTOCOL_VERSION-$MAGIC_STRING-(\\d{6})-(\\d{3})-(.+)")
private val restRegexPattern = Regex("^(\\d{3})-(.+)")

class SMSFormatter {

    private val smsManager = SmsManager.getDefault()

    private fun computeRequestHeaderLength(): Int {

        val baseHeaderContent: ImmutableList<Int> = ImmutableList.from(
            SMS_TUNNEL_PROTOCOL_VERSION.length,
            MAGIC_STRING.length,
            REQUEST_NUMBER_LENGTH,
            FRAGMENT_HEADER_LENGTH
        )

        return baseHeaderContent.fold(0) { acc, i -> acc + i + 1 }
    }

    // Format the input message into SMS packets
    fun formatSMS(
        msg: String,
        currentRequestCounter: Long
    ): MutableList<String> {
        val packets = mutableListOf<String>()

        var packetCount = 1
        var msgIdx = 0
        var currentFragmentSize = 0

        // Compute the string packets with request headers for sending via SMS
        // Each packet will be sent individually

        // Computes the size of the first message header
        val headerSize = computeRequestHeaderLength()

        if (PACKET_SIZE < msg.length + headerSize) {
            val remainderMsgLength = msg.length + headerSize - PACKET_SIZE
            packetCount += kotlin.math.ceil(
                remainderMsgLength.toDouble() / (PACKET_SIZE - FRAGMENT_HEADER_LENGTH)
            ).toInt()
        }

        // creating a list of messages with their headers
        while (msgIdx < msg.length) {
            // First fragment needs a special header
            val requestHeader: String = if (msgIdx == 0) {
                val currentRequestCounterPadded =
                    currentRequestCounter.toString().padStart(REQUEST_NUMBER_LENGTH, '0')
                val fragmentCountPadded =
                    packetCount.toString().padStart(FRAGMENT_HEADER_LENGTH, '0')
                """
                    $SMS_TUNNEL_PROTOCOL_VERSION-
                    $MAGIC_STRING-
                    $currentRequestCounterPadded-
                    $fragmentCountPadded-
                    """.trimIndent().replace("\n", "")
            } else {
                // creating header for consequent messages
                val fragmentNumber =
                    currentFragmentSize.toString().padStart(FRAGMENT_HEADER_LENGTH, '0')
                "$fragmentNumber-"
            }

            // calculating how remaining space after the header is added
            val remainingSpace = PACKET_SIZE - requestHeader.length

            // getting sms string for current fragment based on how much space is remaining
            val currentFragment =
                requestHeader + msg.substring(msgIdx, min(msgIdx + remainingSpace, msg.length))

            // updating msgIdx for next iteration of the loop
            msgIdx = min(msgIdx + remainingSpace, msg.length)

            // add to the list of SMS packets
            packets.add(currentFragment)
            currentFragmentSize += 1
        }

        return packets
    }

    fun sendAckMessage(smsRelayEntity: SmsRelayEntity){
        val phoneNumber = smsRelayEntity.getPhoneNumber()
        val requestIdentifier = smsRelayEntity.getRequestIdentifier()
        val ackFragmentNumber = String.format(
            "%03d",
            smsRelayEntity.numFragmentsReceived - 1
        )

        val ackMessage: String = """
        $SMS_TUNNEL_PROTOCOL_VERSION
        $MAGIC_STRING
        $requestIdentifier
        $ackFragmentNumber
        $SMS_ACK_SUFFIX
        """.trimIndent().replace("\n", "-")

        sendMessage(phoneNumber, ackMessage)
    }

    // Extract the request identifier from an acknowledgment message
    fun getAckRequestIdentifier(ackMessage: String): String {
        return ackRegexPattern.find(ackMessage)?.groupValues!![1]
    }

    // Extract the request identifier from the first message - update this
    fun getNewRequestIdentifier(message: String): String {
        return firstRegexPattern.find(message)?.groupValues!![1]
    }

    // Extract the request identifier from a subsequent message
//    fun getRequestIdentifier(message: String): String {
//        return restRegexPattern.find(message)?.groupValues!![1]
//    }

    // Extract the total number of SMS packets that should be received
    fun getTotalNumOfFragments(message: String): Int {
        return firstRegexPattern.find(message)?.groupValues!![2].toInt()
    }

    // Extract the encrypted content from the first message
    fun getEncryptedDataFromFirstMessage(message: String): String {
        return firstRegexPattern.find(message)?.groupValues!![3]
    }

    // Extract the encrypted content from the subsequent message
    fun getEncryptedDataFromMessage(message: String): String {
        return restRegexPattern.find(message)?.groupValues!![2]
    }

    // Extract the fragment number from an acknowledgment message
    fun getAckFragmentNumber(ackMessage: String): Int {
        return ackRegexPattern.find(ackMessage)?.groupValues!![2].toInt()
    }

    // Check if a message is the first fragment
    fun isFirstMessage(message: String): Boolean {
        return firstRegexPattern.matches(message)
    }

    // Check if a message is an acknowledgment message
    fun isAckMessage(message: String): Boolean {
        return ackRegexPattern.matches(message)
    }

    // Check if a message is a non-first fragment
    fun isRestMessage(message: String): Boolean {
        return restRegexPattern.matches(message)
    }

    // Send a multipart SMS message
    fun sendMessage(phoneNumber: String, smsMessage: String) {
        smsManager.sendMultipartTextMessage(
            phoneNumber,
            null,
            smsManager.divideMessage(smsMessage),
            null,
            null
        )
    }
}
