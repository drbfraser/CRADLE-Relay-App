package com.cradleplatform.cradle_vsa_sms_relay.network

import android.content.SharedPreferences
import android.util.Log
import com.android.volley.Request.Method.GET
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A list of requests type for Volley, Add requests type as needed
 */
class VolleyRequests(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val UNAUTHORIZED = 401
        private const val BAD_REQUEST = 400
        private const val NOT_FOUND = 404
        private const val CONFLICT = 409
        const val ACCESS_TOKEN = "accessToken"
        private const val AUTH = "Authorization"

        private const val MILLISECONDS_PER_SECOND = 1000
        private const val FIVE_MINUTES_IN_SECONDS = 300

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
        jsonBody: JSONObject?,
        callback: (NetworkResult<JSONObject>) -> Unit
    ): JsonObjectRequest {
        val successListener = Response.Listener<JSONObject> { callback(Success(it)) }
        val errorListener = Response.ErrorListener { callback(Failure(it)) }

        return object : JsonObjectRequest(GET, url, jsonBody, successListener, errorListener) {
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

    /*
    * TODO: Make request to `/api/user/auth/refresh_token` to get new access token.
    *  The refresh token is returned from the `auth` endpoint in a cookie, so we will need to
    *  extract it from the cookie, save it in sharedPreferences, then add it back as a cookie for
    *  outgoing requests.
    * */
    private fun refreshAccessToken(accessToken: String): String {
        return accessToken
    }

    /**
     * Decodes the payload of the access token JWT and returns the expiration (exp) claim.
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeAccessTokenExpiration(jwt: String): Long {
        val sections = jwt.split(".")
        val charset = charset("UTF-8")
        val payloadString = String(Base64.UrlSafe.decode(sections[1].toByteArray(charset)), charset)
        val payload = JSONObject(payloadString)
        return payload.getLong("exp")
    }

    @Suppress("TooGenericExceptionCaught")
    private fun verifyAccessToken(): String {
        val accessToken = sharedPreferences.getString(ACCESS_TOKEN, "") ?: return ""
        val exp: Long
        try {
            exp = decodeAccessTokenExpiration(accessToken)
        } catch (e: Exception) {
            Log.e("verifyAccessToken", "Error parsing Access Token : $e")
            return accessToken
        }

        // Get current timestamp in seconds.
        val currentDateTime = java.util.Date()
        val currentTimestamp: Long = currentDateTime.time / MILLISECONDS_PER_SECOND

        Log.i("exp", "$exp")
        Log.i("exp", "$currentTimestamp")

        // If expiration is more than 5 minutes from now, don't do anything.
        if (exp > currentTimestamp - FIVE_MINUTES_IN_SECONDS) return accessToken

        // Access token has expired.
        refreshAccessToken(accessToken)

        // Return the new access token.
        return sharedPreferences.getString(ACCESS_TOKEN, "") ?: ""
    }

    private fun getHttpHeaders(): Map<String, String> {
        val accessToken = verifyAccessToken()
        return mapOf(Pair(AUTH, "Bearer $accessToken"))
    }
}
