package com.cradleplatform.cradle_vsa_sms_relay.model

import javax.inject.Inject

/**
 * Constructs the various URLs required for communicating with the server.
 */
class UrlManager @Inject constructor(val settings: Settings) {

    internal val base: String
        get() {
                val protocol = if (settings.networkUseHttps) "https://" else "http://"
                val hostname = settings.networkHostname
                val port = if (settings.networkPort.isNullOrBlank()) "" else ":${settings.networkPort}"
                return "$protocol$hostname$port"
            }
    val authenticationUrl: String
        get() = "$base/api/user/auth"

    val patientUrl:String
        get() = "$base/api/patients"

    val readingUrl: String
        get() = "$base/api/readings"

    val smsRelayUrl: String
        get() = "$base/api/sms_relay"

    val refreshTokenUrl: String
        get() = "$base/api/user/auth/refresh_token"
}
