package com.cradleplatform.cradle_vsa_sms_relay.managers

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.cradleplatform.cradle_vsa_sms_relay.network.NetworkResult
import com.cradleplatform.cradle_vsa_sms_relay.network.RestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection.HTTP_OK
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user login credentials authenticating with the server
 */
@Singleton
class LoginManager @Inject constructor(
    private val restApi: RestApi,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val TAG = "LoginManager"
        const val ACCESS_TOKEN_KEY = "accessToken"
        const val EMAIL_KEY = "email"
        const val USERNAME_KEY = "username"
        const val USER_ID_KEY = "userId"
    }

    private val loginMutex = Mutex()

    fun isLoggedIn(): Boolean {
        sharedPreferences.run {
            if (!contains(ACCESS_TOKEN_KEY)) {
                return false
            }
            if (!contains(USER_ID_KEY)) {
                return false
            }
        }
        return true
    }

    /**
     * Performs the complete login sequence required to log a user in
     *
     * @param username The username or email to login with.
     * @param password The password to login with.
     * @return [NetworkResult.Success] variant if the user was able to login successfully,
     *  otherwise a [[NetworkResult.Failure] or [[NetworkResult.NetworkException] will be returned
     */
    suspend fun login(
        username: String,
        password: String
    ): NetworkResult<Unit> = withContext(Dispatchers.Default) {
        loginMutex.withLock {
            if (isLoggedIn()) {
                Log.w(TAG, "Trying to login twice!")
                return@withContext NetworkResult.NetworkException(Exception("already logged in"))
            }

            // Send a request to the authentication endpoint to login
            // If we failed to login, return immediately
            val loginResult = restApi.authenticate(username, password)
            if (loginResult is NetworkResult.Success) {
                val loginResponse = loginResult.value
                sharedPreferences.edit(commit = true) {
                    putString(ACCESS_TOKEN_KEY, loginResponse.accessToken)
                    putInt(USER_ID_KEY, loginResponse.user.id)
                    putString(EMAIL_KEY, loginResponse.user.email)
                    putString(USERNAME_KEY, loginResponse.user.username)

                }
            } else {
                return@withContext loginResult.cast()
            }
            return@withContext NetworkResult.Success(Unit, HTTP_OK)
        }
    }

    suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        // Clear all the user specific information from sharedPreferences
        sharedPreferences.edit().remove(ACCESS_TOKEN_KEY).apply()
        sharedPreferences.edit().remove(USER_ID_KEY).apply()
        sharedPreferences.edit().remove(EMAIL_KEY).apply()
        sharedPreferences.edit().remove(USERNAME_KEY).apply()
    }

}

/**
 * Models the response sent back by the server for /api/user/auth.
 */
@Serializable
data class LoginResponse(
    val accessToken: String,
    val user: LoginResponseUser
)

@Serializable
data class LoginResponseUser(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    val name: String?,
    val healthFacilityName: String?,
    val phoneNumbers: List<String>
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String
)
