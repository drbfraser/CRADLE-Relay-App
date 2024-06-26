package com.cradleplatform.cradle_vsa_sms_relay.network

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.UrlManager
import com.cradleplatform.cradle_vsa_sms_relay.network.VolleyRequests.Companion.TOKEN
import org.json.JSONObject
import javax.inject.Inject

/**
 * Responsible for making all the network calls for the application
 */
class NetworkManager(application: Application) {

    val tag = "NetworkManager"

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var urlManager: UrlManager

    private val volleyRequestQueue: VolleyRequestQueue by lazy { VolleyRequestQueue(application) }

    private val volleyRequests: VolleyRequests

    init {
        (application as MyApp).component.inject(this)
        volleyRequests = VolleyRequests(sharedPreferences)
    }

    /**
     * authenticate the user and save the TOKEN/ username
     * @param email email address for the user
     * @param password for the user
     * @param callBack a boolean callback to know whether request was successful or not
     */
    fun authenticateTheUser(email: String, password: String, callBack: BooleanCallback) {
        val jsonObject = JSONObject()
        jsonObject.put("email", email)
        jsonObject.put("password", password)
        val request =
            volleyRequests.postJsonObjectRequest(urlManager.authenticationUrl, jsonObject) { result ->
                when (result) {
                    is Success -> {
                        // save the user credentials
                        val json = result.unwrap()
                        val editor = sharedPreferences.edit()
                        editor.putString(TOKEN, json.getString(TOKEN))
                        editor.putString(USER_ID, json.getString("userId"))
                        editor.putString(LOGIN_EMAIL, email)
                        editor.putInt(LOGIN_PASSWORD, password.hashCode())
                        editor.apply()
                        // let the calling object know result
                        callBack(true)
                    }
                    is Failure -> {
                        result.value.printStackTrace()
                        callBack(false)
                    }
                }
            }
        volleyRequestQueue.addRequest(request)
    }

    companion object {
        const val USER_ID = "userId"
        const val LOGIN_EMAIL = "email"
        const val LOGIN_PASSWORD = "password"
    }
}
