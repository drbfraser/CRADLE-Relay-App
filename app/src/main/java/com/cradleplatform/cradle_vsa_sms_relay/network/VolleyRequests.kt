package com.cradleplatform.cradle_vsa_sms_relay.network

import android.content.SharedPreferences
import com.android.volley.Request.Method.GET
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * A list of requests type for Volley, Add requests type as needed
 */
class VolleyRequests(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val UNAUTHORIZED = 401
        private const val BAD_REQUEST = 400
        private const val NOT_FOUND = 404
        private const val CONFLICT = 409
        const val TOKEN = "token"
        private const val AUTH = "Authorization"

        @Suppress("ComplexMethod")
        fun getServerErrorMessage(error: VolleyError): String {
            var message = "Unable to upload to server (network error)"
            when {
                error.cause != null -> {
                    message = when (error.cause) {
                        UnknownHostException::class.java -> {
                            "Unable to resolve server address; check server URL in settings."
                        }
                        ConnectException::class.java -> {
                            "Cannot reach server; check network connection."
                        }
                        else -> {
                            error.cause?.message.toString()
                        }
                    }
                }
                error.networkResponse != null -> {
                    message = when (error.networkResponse.statusCode) {
                        UNAUTHORIZED -> "Server rejected credentials; Log in with correct credentials"
                        BAD_REQUEST -> "Server rejected upload request; make sure referral is correctly formatted"
                        NOT_FOUND -> "Server rejected URL; Contact the developers for support"
                        CONFLICT -> "The referral already exists in the server"
                        else ->
                            "Server rejected upload; Contact developers." +
                                " Code " + error.networkResponse.statusCode
                    }
                }
            }
            return message
        }
    }

    /**
     * returns a [GET] [JsonObjectRequest] type request
     */
    fun getJsonObjectRequest(
        url: String,
        jsonaBody: JSONObject?,
        callback: (NetworkResult<JSONObject>) -> Unit
    ): JsonObjectRequest {
        val successListener = Response.Listener<JSONObject> { callback(Success(it)) }
        val errorListener = Response.ErrorListener { callback(Failure(it)) }

        return object : JsonObjectRequest(GET, url, jsonaBody, successListener, errorListener) {
            /**
             * Passing some request headers
             */
            override fun getHeaders(): Map<String, String>? {
                return getHttpHeaders()
            }
        }
    }

    /**
     * returns a [POST] [JsonObjectRequest] type request
     */
    fun postJsonObjectRequest(
        url: String,
        jsonBody: JSONObject?,
        callback: (NetworkResult<JSONObject>) -> Unit
    ): JsonObjectRequest {
        val successListener = Response.Listener<JSONObject> { callback(Success(it)) }
        val errorListener = Response.ErrorListener { callback(Failure(it)) }
        return object : JsonObjectRequest(POST, url, jsonBody, successListener, errorListener) {
            /**
             * Passing some request headers
             */
            override fun getHeaders(): Map<String, String>? {
                return getHttpHeaders()
            }
        }
    }

    private fun getHttpHeaders(): Map<String, String> {
        val token = sharedPreferences.getString(TOKEN, "")
        return mapOf(Pair(AUTH, "Bearer $token"))
    }
}

object Urls {
//    private const val base = "cradleplatform.com/api"
    private const val base = "10.0.2.2:5000/api"
    private const val protocol = "http://"

    const val authenticationUrl: String = "$protocol$base/user/auth"

    const val patientUrl = "$protocol$base/patients"

    const val readingUrl = "$protocol$base/readings"
}
