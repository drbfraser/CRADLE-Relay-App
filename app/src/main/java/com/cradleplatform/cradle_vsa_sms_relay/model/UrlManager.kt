package com.cradleplatform.cradle_vsa_sms_relay.model

import android.util.Log
import javax.inject.Inject

class UrlManager @Inject constructor(val settings: Settings) {
    val TAG = "UrlManager"

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
}