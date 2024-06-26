package com.cradleplatform.cradle_vsa_sms_relay.network

import android.accounts.NetworkErrorException
import com.android.volley.VolleyError

/**
 *Base class for all network responses.
 */
sealed class NetworkResult<T>

/**
 * An extension of the [NetworkResult] for success
 */
data class Success<T>(val value: T) : NetworkResult<T>()

/**
 * An extension of the [NetworkResult] for Failure
 */
data class Failure<T>(val value: VolleyError) : NetworkResult<T>()

/**
 * Extension function to map Network results
 */
fun <T, U> NetworkResult<T>.map(f: (T) -> U): NetworkResult<U> = when (this) {
    is Success -> Success(f(this.value))
    is Failure -> Failure(this.value)
}

/**
 * Extension function to unwrap the [Success] result
 */
fun <T> NetworkResult<T>.unwrap(): T = when (this) {
    is Success -> this.value
    is Failure -> throw NetworkErrorException("unwrap of failure network result")
}

/**
 * Extension function to unwrap the [Failure] result
 */
fun <T> NetworkResult<T>.unwrapFailure(): VolleyError = when (this) {
    is Success -> throw NetworkErrorException("unwrap failure of success network result")
    is Failure -> this.value
}

/**
 * typaliases for all the callbacks to make them more readable and shorter
 */
typealias BooleanCallback = (isSuccessful: Boolean) -> Unit
