package com.cradleplatform.cradle_vsa_sms_relay.utilities

import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.google.firebase.crashlytics.internal.model.ImmutableList

private const val PROTOCOL_VERSION = "01"
// private val FIRST_FRAGMENT_REGEX_PATTERN = Regex("^${PROTOCOL_VERSION}-CRADLE-\\d{6}-\\d{3}.*")
private const val NUM_OF_FIRST_PACKET_COMPONENTS = 5
private const val NUM_OF_NON_FIRST_PACKET_COMPONENTS = 2

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
//            return HTTPSRequest(smsHttpRequest.phoneNumber, smsHttpRequest.encryptedFragments.joinToString())
            //TODO currently use admin number for now
            return HTTPSRequest("+1-123-456-7890", smsHttpRequest.encryptedFragments.joinToString(""))
        }
    }
}
