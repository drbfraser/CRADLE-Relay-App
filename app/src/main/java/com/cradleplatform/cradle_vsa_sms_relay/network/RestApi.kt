package com.cradleplatform.cradle_vsa_sms_relay.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.cradleplatform.cradle_vsa_sms_relay.managers.LoginResponse
import com.cradleplatform.cradle_vsa_sms_relay.managers.RefreshTokenResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayRequestBody
import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayResponseBody
import com.cradleplatform.cradle_vsa_sms_relay.model.UrlManager
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.json.JSONObject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Provides type-safe methods for interacting with the CRADLE server API.
 *
 * Each method is written as a `suspend` function which is executed using the
 * [IO] dispatcher and returns a [NetworkResult]. In general, a method will
 * return a [NetworkResult.Success] variant with the desired return value wrapped inside if
 * the server was able to successfully respond to the request. A [NetworkResult.Failure]
 * return value means that the request made it to the server, but the server
 * responded with an error and was not able to complete the request. This
 * happens when the requested resource cannot be found for example. A
 * [NetworkResult.NetworkException] return value means that the networking driver ([Http])
 * threw an exception when sending the request or handling the response.
 * A timeout is one such cause of an exception for example.
 */
@OptIn(ExperimentalSerializationApi::class)
@Singleton
class RestApi(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val urlManager: UrlManager,
    private val http: Http,
) {

    companion object {
        private const val TAG = "RestApi"
        private const val MILLISECONDS_PER_SECOND = 1000
        private const val FIVE_MINUTES_IN_SECONDS = 300
    }

    private val jsonBuilderWithIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

    /**
     * Sends a request to the authentication API endpoint to log a user in.
     *
     * @param username The user's email or username.
     * @param password The user's password.
     * @return If successful, the [LoginResponse] that was returned by the server
     *  which contains a bearer token to authenticate the user.
     */
    suspend fun authenticate(
        username: String,
        password: String,
    ): NetworkResult<LoginResponse> = withContext(IO) {
        val body = JSONObject().put("username", username).put("password", password).toString()
            .encodeToByteArray()

        val method = Http.Method.POST
        val url = urlManager.authenticationUrl
        val headers = mapOf<String, String>()

        http.makeRequest(
            method = method,
            url = url,
            headers = headers,
            requestBody = buildJsonRequestBody(body),
            inputStreamReader = {
                jsonBuilderWithIgnoreUnknownKeys.decodeFromStream<LoginResponse>(it)
            })
    }

    /**
     * Sends message that was received by SMS to the `/api/sms_relay` endpoint on the server.
     */
    suspend fun relaySmsRequest(
        httpRelayRequestBody: HttpRelayRequestBody
    ): NetworkResult<HttpRelayResponseBody> = withContext(IO) {
        val body = Json.encodeToString(httpRelayRequestBody).encodeToByteArray()

        http.makeRequest(
            method = Http.Method.POST,
            url = urlManager.smsRelayUrl,
            headers = makeAuthorizationHeader(),
            requestBody = buildJsonRequestBody(body),
            inputStreamReader = {
                Json.decodeFromStream<HttpRelayResponseBody>(it)
            })
    }

    /**
     * Makes a request to the server to refresh the access token.
     */
    private suspend fun refreshAccessToken(accessToken: String): String? = withContext(IO) {
        val method = Http.Method.POST
        val url = urlManager.refreshTokenUrl
        val username = sharedPreferences.getString("username", null)

        // Must send username in body of request.
        val body = JSONObject().put("username", username).toString()
            .encodeToByteArray()

        val headers = mapOf("Authorization" to "Bearer $accessToken")

        val result = http.makeRequest(method = method,
            url = url,
            headers = headers,
            requestBody = buildJsonRequestBody(body),

            inputStreamReader = {
                Json.decodeFromStream<RefreshTokenResponse>(it)
            })

        if (result is NetworkResult.Success<RefreshTokenResponse>) {
            val newAccessToken = result.value.accessToken
            sharedPreferences.edit(commit = true) {
                putString("accessToken", newAccessToken)
            }
            return@withContext newAccessToken
        } else {
            val errorMessage = result.getStatusMessage()
            Log.e(TAG, "Failed to refresh access token:")
            if (errorMessage != null) {
                Log.e(TAG, "$errorMessage")
            }
            return@withContext null
        }
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
    private suspend fun getAccessToken(): String? {
        val accessToken = sharedPreferences.getString("accessToken", null) ?: return null
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

        // If expiration is more than 5 minutes from now, don't do anything.
        if (exp > currentTimestamp - FIVE_MINUTES_IN_SECONDS) return accessToken
        Log.e("verifyAccessToken", "ACCESS TOKEN IS EXPIRED!")

        // Access token has expired.
        return refreshAccessToken(accessToken)
    }

    private suspend fun makeAuthorizationHeader(): Map<String, String> {
        val accessToken = getAccessToken()
        return if (accessToken != null) {
            mapOf("Authorization" to "Bearer $accessToken")
        } else {
            mapOf()
        }
    }

}