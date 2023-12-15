package com.cradleplatform.cradle_vsa_sms_relay.utilities

import android.telephony.SmsManager
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import kotlin.math.min

/**
 * class handles the parsing and formatting of all messages that are sent and received
 * class is used to retrieve information from the SMS message based on the protocol being used
 */

private const val PACKET_SIZE = 153 * 2
// private const val MAX_PACKET_NUMBER = 99

// Fixed strings, prefixes, suffixes involved in the SMS Protocol
private const val SMS_TUNNEL_PROTOCOL_VERSION = "01"
private const val SMS_ACK_SUFFIX = "ACK"
private const val MAGIC_STRING = "CRADLE"
private const val REPLY_SUCCESS = "REPLY"
private const val REPLY_ERROR = "REPLY_ERROR"
private const val REPLY_ERROR_CODE_PREFIX = "ERR"

// Lengths for different parts of the SMS Protocol
private const val REPLY_ERROR_CODE_LENGTH = 3
private const val FRAGMENT_HEADER_LENGTH = 3
private const val REQUEST_NUMBER_LENGTH = 6

// positions of request identifiers inside different messages of the SMS protocol
private const val POS_FIRST_MSG_REQUEST_COUNTER = 1
private const val POS_ACK_MSG_REQUEST_COUNTER = 1
private const val POS_REPLY_SUCCESS_REQUEST_COUNTER = 1
private const val POS_REPLY_ERROR_REQUEST_COUNTER = 1

// positions for data inside different messages of the SMS protocol
private const val POS_FIRST_MSG_DATA = 3
private const val POS_REST_MSG_DATA = 2
private const val POS_REPLY_SUCCESS_DATA = 3
private const val POS_REPLY_ERROR_DATA = 4

// positions for total fragments in transaction
private const val POS_FIRST_NUM_FRAGMENTS = 2
private const val POS_REPLY_SUCCESS_NUM_FRAGMENTS = 2
private const val POS_REPLY_ERROR_NUM_FRAGMENTS = 2

// positions for current fragment number
private const val POS_ACK_CURR_FRAGMENT = 2
private const val POS_REST_CURR_FRAGMENT = 1

private val ackRegexPattern =
    Regex(
        "^$SMS_TUNNEL_PROTOCOL_VERSION-$MAGIC_STRING-" +
                "(\\d{$REQUEST_NUMBER_LENGTH})-(\\d{$FRAGMENT_HEADER_LENGTH})-$SMS_ACK_SUFFIX$"
    )

private val firstRegexPattern =
    Regex(
        "^$SMS_TUNNEL_PROTOCOL_VERSION-$MAGIC_STRING-" +
                "(\\d{$REQUEST_NUMBER_LENGTH})-(\\d{$FRAGMENT_HEADER_LENGTH})-(.+)"
    )

private val restRegexPattern =
    Regex(
        "^(\\d{$FRAGMENT_HEADER_LENGTH})-(.+)"
    )

private val firstErrorReplyPattern =
    Regex(
        "^$SMS_TUNNEL_PROTOCOL_VERSION-$MAGIC_STRING-" +
                "(\\d{$REQUEST_NUMBER_LENGTH})-$REPLY_ERROR-(\\d{$FRAGMENT_HEADER_LENGTH})-" +
                "$REPLY_ERROR_CODE_PREFIX(\\d{$REPLY_ERROR_CODE_LENGTH})-(.+)$"
    )

private val firstSuccessReplyPattern =
    Regex(
        "^$SMS_TUNNEL_PROTOCOL_VERSION-$MAGIC_STRING-" +
                "(\\d{$REQUEST_NUMBER_LENGTH})-$REPLY_SUCCESS-(\\d{$FRAGMENT_HEADER_LENGTH})-(.+)"
    )

@Suppress("LargeClass", "TooManyFunctions")
class SMSFormatter {

    private val smsManager = SmsManager.getDefault()

    // function to calculate the size of the first reply packet
    private fun computeRequestHeaderLength(isSuccessful: Boolean): Int {
        val baseHeaderContent: MutableList<Int> = mutableListOf(
            SMS_TUNNEL_PROTOCOL_VERSION.length,
            MAGIC_STRING.length,
            REQUEST_NUMBER_LENGTH,
            FRAGMENT_HEADER_LENGTH
        )

        // Add the correct header to the list based on whether the HTTP request was successful or not
        if (isSuccessful)
            baseHeaderContent.add(0, REPLY_SUCCESS.length)
        else {
            baseHeaderContent.add(0, REPLY_ERROR.length)
            baseHeaderContent.add(0, REPLY_ERROR_CODE_PREFIX.length)
            baseHeaderContent.add(0, REPLY_ERROR_CODE_LENGTH)
        }

        return baseHeaderContent.fold(0) { acc, i -> acc + i + 1 }
    }

    // Format the input message into SMS packets
    fun formatSMS(
        msg: String,
        currentRequestCounter: Long,
        isSuccessful: Boolean,
        statusCode: Int?
    ): MutableList<String> {
        val packets = mutableListOf<String>()

        var packetCount = 1
        var msgIdx = 0
        var currentFragmentSize = 0

        // Compute the string packets with request headers for sending via SMS
        // Each packet will be sent individually

        // Computes the size of the first message header
        val headerSize = computeRequestHeaderLength(isSuccessful)

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
                    ${if (isSuccessful) REPLY_SUCCESS else REPLY_ERROR}-
                    $fragmentCountPadded-
                    ${if (isSuccessful) "" else REPLY_ERROR_CODE_PREFIX + statusCode.toString() + "-"}
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

    fun sendAckMessage(smsRelayEntity: SmsRelayEntity) {
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
        return ackRegexPattern.find(ackMessage)?.groupValues!![POS_ACK_MSG_REQUEST_COUNTER]
    }

    // Extract the request identifier from the first message - update this
    fun getNewRequestIdentifier(message: String): String {
        return firstRegexPattern.find(message)?.groupValues!![POS_FIRST_MSG_REQUEST_COUNTER]
    }

    // Extract the total number of SMS packets that should be received
    fun getTotalNumOfFragments(message: String): Int {
        return firstRegexPattern.find(message)?.groupValues!![POS_FIRST_NUM_FRAGMENTS].toInt()
    }

    // Extract the encrypted content from the first message
    fun getEncryptedDataFromFirstMessage(message: String): String {
        return firstRegexPattern.find(message)?.groupValues!![POS_FIRST_MSG_DATA]
    }

    // Extract the encrypted content from the subsequent message
    fun getEncryptedDataFromRestMessage(message: String): String {
        return restRegexPattern.find(message)?.groupValues!![POS_REST_MSG_DATA]
    }

    // Extract the fragment number from an acknowledgment message
    fun getAckFragmentNumber(ackMessage: String): Int {
        return ackRegexPattern.find(ackMessage)?.groupValues!![POS_ACK_CURR_FRAGMENT].toInt()
    }

    // Extract the fragment number from a subsequent messagex`
    fun getRestFragmentNumber(ackMessage: String): Int {
        return ackRegexPattern.find(ackMessage)?.groupValues!![POS_REST_CURR_FRAGMENT].toInt()
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

    fun getEncryptedData(smsRelayEntity: SmsRelayEntity): String {
        var encryptedData = ""
        smsRelayEntity.smsPacketsFromMobile.forEach {
            if (isFirstMessage(it)) {
                encryptedData = getEncryptedDataFromFirstMessage(it)
            } else {
                encryptedData += getEncryptedDataFromRestMessage(it)
            }
        }
        return encryptedData
    }
}
