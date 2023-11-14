package com.cradleplatform.cradle_vsa_sms_relay.utilities

import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.google.firebase.crashlytics.internal.model.ImmutableList
import kotlin.math.min

private const val PROTOCOL_VERSION = "01"
// private val FIRST_FRAGMENT_REGEX_PATTERN = Regex("^${PROTOCOL_VERSION}-CRADLE-\\d{6}-\\d{3}.*")
private const val NUM_OF_FIRST_PACKET_COMPONENTS = 5
private const val NUM_OF_NON_FIRST_PACKET_COMPONENTS = 2

private const val PACKET_SIZE = 153 * 2
private const val SMS_TUNNEL_PROTOCOL_VERSION = "01"
private const val MAGIC_STRING = "CRADLE"
private const val FRAGMENT_HEADER_LENGTH = 3
private const val REQUEST_NUMBER_LENGTH = 6

class SMSFormatter {

    companion object {

        fun isSMSPacketFirstFragment(packetMessage: String): Boolean {
            return packetMessage.startsWith("$PROTOCOL_VERSION-CRADLE")
        }

        fun decomposeSMSPacket(packetMessage: String, isFirstPacket: Boolean): ImmutableList<String> {
            val packetComponents: List<String> = if (isFirstPacket) {
                packetMessage.split('-', limit = NUM_OF_FIRST_PACKET_COMPONENTS)
            } else {
                packetMessage.split('-', limit = NUM_OF_NON_FIRST_PACKET_COMPONENTS)
            }
            return ImmutableList.from(packetComponents)
        }

        fun convertSMSHttpRequestToHttpsRequest(smsHttpRequest: SMSHttpRequest): HTTPSRequest {
            return HTTPSRequest(smsHttpRequest.phoneNumber, smsHttpRequest.encryptedFragments.joinToString(""))
        }
    }

    private fun computeRequestHeaderLength(): Int {

        val baseHeaderContent: ImmutableList<Int> = ImmutableList.from(
            SMS_TUNNEL_PROTOCOL_VERSION.length,
            MAGIC_STRING.length,
            REQUEST_NUMBER_LENGTH,
            FRAGMENT_HEADER_LENGTH
        )

        return baseHeaderContent.fold(0) { acc, i -> acc + i + 1 }
    }

    fun formatSMS(
        msg: String,
        currentRequestCounter: Long
    ): MutableList<String> {
        val packets = mutableListOf<String>()

        var packetCount = 1
        var msgIdx = 0
        var currentFragmentSize = 0

        // first compute the number of fragment required for the input message
        val headerSize = computeRequestHeaderLength()

        if (PACKET_SIZE < msg.length + headerSize) {
            val remainderMsgLength = msg.length + headerSize - PACKET_SIZE
            packetCount += kotlin.math.ceil(
                remainderMsgLength.toDouble() / (PACKET_SIZE - FRAGMENT_HEADER_LENGTH)
            ).toInt()
        }

        while (msgIdx < msg.length) {
            // first fragment needs special header
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
                val fragmentNumber =
                    currentFragmentSize.toString().padStart(FRAGMENT_HEADER_LENGTH, '0')
                "$fragmentNumber-"
            }
            val remainingSpace = PACKET_SIZE - requestHeader.length
            val currentFragment =
                requestHeader + msg.substring(msgIdx, min(msgIdx + remainingSpace, msg.length))
            msgIdx = min(msgIdx + remainingSpace, msg.length)

            packets.add(currentFragment)
            currentFragmentSize += 1
        }

        return packets
    }

}
