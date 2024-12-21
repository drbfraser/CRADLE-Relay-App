package com.cradleplatform.cradle_vsa_sms_relay.network

import android.app.Application
import android.content.SharedPreferences
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.UrlManager
import com.cradleplatform.cradle_vsa_sms_relay.network.VolleyRequests.Companion.ACCESS_TOKEN
import org.json.JSONObject
import javax.inject.Inject

/**
 * Responsible for making all the network calls for the application
 */
class NetworkManager(application: Application) {

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
     * @param username Username or email address for the user.
     * @param password Password of the user.
     * @param callBack A boolean callback to know whether request was successful or not.
     */
    fun authenticateTheUser(username: String, password: String, callBack: BooleanCallback) {
        val jsonObject = JSONObject()
        /* Either email or username can be used to login, but whichever one is used is passed to
        * the server as 'username'. */
        jsonObject.put("username", username)
        jsonObject.put("password", password)
        val request =
            volleyRequests.postJsonObjectRequest(urlManager.authenticationUrl, jsonObject) { result ->
                when (result) {
                    is Success -> {
                        // save the user credentials
                        val json = result.unwrap()
                        val editor = sharedPreferences.edit()
                        editor.putString(ACCESS_TOKEN, json.getString(ACCESS_TOKEN))
                        editor.putString(USER_ID, json.getString("userId"))
                        editor.putString(LOGIN_EMAIL, username)
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
