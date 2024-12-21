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
        `when`(mockSharedPreferences.getString(VolleyRequests.ACCESS_TOKEN, "")).thenReturn("test-token")
        volleyRequests = VolleyRequests(mockSharedPreferences)
    }

    @Test
    fun testGetServerErrorMessage() {
        val unauthorized = VolleyError(NetworkResponse(401, null, false, 0L, listOf()))
        val badRequest = VolleyError(NetworkResponse(400, null, false, 0L, listOf()))
        val notFound = VolleyError(NetworkResponse(404, null, false, 0L, listOf()))
        val conflict = VolleyError(NetworkResponse(409, null, false, 0L, listOf()))
        val notDefined = VolleyError(NetworkResponse(500, null, false, 0L, listOf()))
        assertEquals("Server rejected credentials; Log in with correct credentials", VolleyRequests.getServerErrorMessage(unauthorized))
        assertEquals("Server rejected upload request; make sure referral is correctly formatted", VolleyRequests.getServerErrorMessage(badRequest))
        assertEquals("Server rejected URL; Contact the developers for support", VolleyRequests.getServerErrorMessage(notFound))
        assertEquals("The referral already exists in the server", VolleyRequests.getServerErrorMessage(conflict))
        assertEquals("Server rejected upload; Contact developers. Code 500", VolleyRequests.getServerErrorMessage(notDefined))
    }

    @Test
    fun testGetHeaders() {
        val headers = volleyRequests.getJsonObjectRequest(Urls.authenticationUrl, null) {}.headers
        assertEquals("Bearer test-token", headers!!["Authorization"])
    }

    @Test
    fun testGetJsonObjectRequest() {
        val jsonBody = JSONObject().apply { put("key", "value")}
        val request1 = volleyRequests.getJsonObjectRequest(Urls.authenticationUrl, null) {}
        val request2 = volleyRequests.getJsonObjectRequest(Urls.patientUrl, jsonBody) {}
        val request3 = volleyRequests.getJsonObjectRequest(Urls.readingUrl, null) {}
        assertEquals(Request.Method.GET, request1.method)
        assertEquals(Request.Method.GET, request2.method)
        assertEquals(Request.Method.GET, request3.method)
        assertEquals(null, request1.body)
        assertEquals(jsonBody.toString(), request2.body.toString(Charsets.UTF_8))
        assertEquals(null, request3.body)
        assertEquals(Urls.authenticationUrl, request1.url)
        assertEquals(Urls.patientUrl, request2.url)
        assertEquals(Urls.readingUrl, request3.url)
    }

    @Test
    fun testPostJsonObjectRequest() {
        val jsonBody = JSONObject().apply { put("key", "value")}
        val request1 = volleyRequests.postJsonObjectRequest(Urls.authenticationUrl, jsonBody) {}
        val request2 = volleyRequests.postJsonObjectRequest(Urls.patientUrl, null) {}
        val request3 = volleyRequests.postJsonObjectRequest(Urls.readingUrl, jsonBody) {}
        assertEquals(Request.Method.POST, request1.method)
        assertEquals(Request.Method.POST, request2.method)
        assertEquals(Request.Method.POST, request3.method)
        assertEquals(jsonBody.toString(), request1.body.toString(Charsets.UTF_8))
        assertEquals(null, request2.body)
        assertEquals(jsonBody.toString(), request3.body.toString(Charsets.UTF_8))
        assertEquals(Urls.authenticationUrl, request1.url)
        assertEquals(Urls.patientUrl, request2.url)
        assertEquals(Urls.readingUrl, request3.url)
    }
}
