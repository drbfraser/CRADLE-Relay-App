package com.cradle.cradle_vsa_sms_relay.network

import android.app.Application
import android.content.SharedPreferences
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradle.cradle_vsa_sms_relay.network.Urls.authenticationUrl
import com.cradle.cradle_vsa_sms_relay.network.VolleyRequests.Companion.TOKEN
import javax.inject.Inject
import org.json.JSONObject

/**
 * Responsible for making all the network calls for the application
 */
class NetworkManager(application: Application) {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

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
            volleyRequests.postJsonObjectRequest(authenticationUrl, jsonObject) { result ->
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

    /**
     * returns patient information, given the id
     */
    private fun uploadPatient(patientJSONObject: JSONObject, callback: (NetworkResult<JSONObject>) -> Unit) {
        val request = volleyRequests.postJsonObjectRequest(Urls.patientUrl, patientJSONObject) { result ->
            when (result) {
                is Success -> {
                    callback(Success(result.value))
                }
                is Failure -> {
                    // update database with error
                    callback(Failure(result.value))
                }
            }
        }
        volleyRequestQueue.addRequest(request)
    }

    /**
     * send a reading to the server and propagates its result down to the client
     * @param smsReferralEntity the reading should be inside this referral entity
     * @param callback callback for the caller
     */
    private fun uploadReadingToTheServer(
        smsReferralEntity: SmsReferralEntity,
        callback: (NetworkResult<JSONObject>) -> Unit
    ) {

        // parse the patient
        val patientJSONObject = JSONObject(smsReferralEntity.jsonData.toString()).getJSONObject("patient")
        // parse the reading
        val readingJson = patientJSONObject.getJSONArray("readings")[0] as JSONObject
        val request =
            volleyRequests.postJsonObjectRequest(Urls.readingUrl, readingJson) { result ->
                when (result) {
                    is Success -> {
                        callback(Success(result.value))
                    }
                    is Failure -> {
                        callback(Failure(result.value))
                    }
                }
            }
        volleyRequestQueue.addRequest(request)
    }

    /**
     * uploads a single referral and let the caller know upload status
     * @param smsReferralEntity the patient exists within this entity
     * @param callback callback for the caller
     */
    fun uploadReferral(smsReferralEntity: SmsReferralEntity, callback: (NetworkResult<JSONObject>) -> Unit) {
        val patientJSONObject = JSONObject(smsReferralEntity.jsonData.toString())
            .getJSONObject("patient")

        uploadPatient(patientJSONObject) { result ->
            when (result) {
                is Success -> {
                    // let caller know we uploaded referral
                    callback(Success(result.value))
                }
                is Failure -> {
                    // upload reading only since patient exists.
                    uploadReadingToTheServer(smsReferralEntity, callback)
                }
            }
        }
    }

    companion object {
        const val USER_ID = "userId"
        const val LOGIN_EMAIL = "email"
        const val LOGIN_PASSWORD = "password"
    }
}
