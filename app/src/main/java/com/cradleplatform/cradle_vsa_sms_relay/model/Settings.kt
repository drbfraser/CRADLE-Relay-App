package com.cradleplatform.cradle_vsa_sms_relay.model

import android.content.Context
import android.content.SharedPreferences
import com.cradleplatform.cradle_vsa_sms_relay.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds app-wide settings which are persisted in Android's shared preference.
 */
@Singleton
open class Settings @Inject constructor(
    val sharedPreferences: SharedPreferences,
    val context: Context
) {
    open val networkHostname:String?
        get() = sharedPreferences.getString(context.getString(R.string.key_server_hostname),
            context.getString(R.string.settings_default_server_hostname))

    open val networkPort:String?
        get() = sharedPreferences.getString(context.getString(R.string.key_server_port),
            context.getString(R.string.settings_default_server_port))

    open val networkUseHttps:Boolean
        get() = sharedPreferences.getBoolean(context.getString(R.string.key_server_use_https),
            true)
}
