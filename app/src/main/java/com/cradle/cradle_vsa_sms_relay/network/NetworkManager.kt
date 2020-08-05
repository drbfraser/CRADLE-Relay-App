package com.cradle.cradle_vsa_sms_relay.network

import android.app.Application
import android.content.SharedPreferences
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradle.neptune.network.VolleyRequests
import com.cradle.neptune.network.VolleyRequests.Companion.TOKEN
import org.json.JSONObject
import javax.inject.Inject

class NetworkManager(application: Application) {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val volleyRequestQueue:VolleyRequestQueue  by lazy { VolleyRequestQueue(application) }

    private val volleyRequests:VolleyRequests

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

    private fun getPatientOnlyInfo(id: String, callback: (NetworkResult<JSONObject>) -> Unit) {
        val request = volleyRequests.getJsonObjectRequest("$patientById$id/info", null) {
            when (it) {

                is Success -> {
                    // patient already exists, post reading
                }
            }
            callback(it)
        }
        volleyRequestQueue.addRequest(request)
    }

    /**
     * send a reading to the server and propogates its result down to the client
     * @param reading reading to upload
     * @param callback callback for the caller
     */
    private fun uploadReadingToTheServer(smsReferralEntity: SmsReferralEntity) {
        val patientJSONObject = JSONObject(smsReferralEntity.jsonData.toString()).getJSONObject("patient")
        val readingJson = patientJSONObject.getJSONArray("readings")[0] as JSONObject
        val request =
            volleyRequests.postJsonObjectRequest(reading, readingJson) { result ->
                when (result) {
                    is Success -> {
                        // update database
                    }
                    is Failure -> {
                        //update database with error
                    }
                }
            }
        volleyRequestQueue.addRequest(request)
    }

    fun uploadReferral(smsReferralEntity: SmsReferralEntity){
        val patientJSONObject = JSONObject(smsReferralEntity.jsonData.toString()).getJSONObject("patient")
        val patientId:String = patientJSONObject.getString("id")

        getPatientOnlyInfo(patientId){result ->
            when (result) {
                is Success -> {
                    // update the database
                }
                is Failure -> {
                    uploadReadingToTheServer(smsReferralEntity)
                }
            }
        }
    }


    companion object {
        const val USER_ID = "userId"
        const val LOGIN_EMAIL = "email"
        const val LOGIN_PASSWORD = "password"
        const val authenticationUrl = "http://10.0.2.2:5000/api/user/auth"
        const val patientById = "http://10.0.2.2:5000/api/patients"
        const val reading = "http://10.0.2.2:5000/api/readings"
    }

}