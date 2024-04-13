package com.cradleplatform.cradle_vsa_sms_relay.model

data class HTTPSResponseSent(
    val relayEntity: SmsRelayEntity,
    val phoneNumber: String,
    val lastEncryptedPacket: String,
    val lastEncryptedPacketNum: Int = 0,
    var timestamp: Long = System.currentTimeMillis() + DEFAULT_WAIT,
    var numberOfRetries: Int = 0
) : Comparable<HTTPSResponseSent> {
    override fun compareTo(other: HTTPSResponseSent): Int {
        return this.timestamp.compareTo(other.timestamp)
    }

    companion object {
        const val MAX_RETRIES = 5
        const val DEFAULT_WAIT = 3000L
        const val MAX_WAIT = 30000.0
    }
}
