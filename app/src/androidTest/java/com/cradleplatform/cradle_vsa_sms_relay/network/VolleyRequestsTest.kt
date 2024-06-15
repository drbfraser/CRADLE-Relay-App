package com.cradleplatform.cradle_vsa_sms_relay.network

import com.android.volley.Request
import com.android.volley.VolleyError
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import android.content.SharedPreferences
import com.android.volley.NetworkResponse

@RunWith(MockitoJUnitRunner::class)
class VolleyRequestsTest {

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    private lateinit var volleyRequests: VolleyRequests

    @Before
    fun setUp() {
        `when`(mockSharedPreferences.getString(VolleyRequests.TOKEN, "")).thenReturn("test-token")
        volleyRequests = VolleyRequests(mockSharedPreferences)
    }

    @Test
    fun testGetServerErrorMessage() {
        val unauthorized = VolleyError(
            NetworkResponse(401, null, false, 0L, listOf())
        )
        assertEquals("Server rejected credentials; Log in with correct credentials", VolleyRequests.getServerErrorMessage(unauthorized))
        val badRequest = VolleyError(
            NetworkResponse(400, null, false, 0L, listOf())
        )
        assertEquals("Server rejected upload request; make sure referral is correctly formatted", VolleyRequests.getServerErrorMessage(badRequest))
        val notFound = VolleyError(
            NetworkResponse(404, null, false, 0L, listOf())
        )
        assertEquals("Server rejected URL; Contact the developers for support", VolleyRequests.getServerErrorMessage(notFound))
        val conflict = VolleyError(
            NetworkResponse(409, null, false, 0L, listOf())
        )
        assertEquals("The referral already exists in the server", VolleyRequests.getServerErrorMessage(conflict))
        val notIncluded = VolleyError(
            NetworkResponse(500, null, false, 0L, listOf())
        )
        assertEquals("Server rejected upload; Contact developers. Code 500", VolleyRequests.getServerErrorMessage(notIncluded))
    }

    @Test
    fun testGetHeaders() {
        val headers = volleyRequests.getJsonObjectRequest(Urls.authenticationUrl, null) {}.headers
        assertEquals("Bearer test-token", headers!!["Authorization"])
    }

    @Test
    fun testGetJsonObjectRequest() {
        val request = volleyRequests.getJsonObjectRequest(Urls.authenticationUrl, null) {}
        assertEquals(Request.Method.GET, request.method)
    }

    @Test
    fun testPostJsonObjectRequest() {
        val request = volleyRequests.postJsonObjectRequest(Urls.authenticationUrl, JSONObject()) {}
        assertEquals(Request.Method.POST, request.method)
    }
}
