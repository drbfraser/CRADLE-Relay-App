package com.cradleplatform.cradle_vsa_sms_relay.model

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UrlManagerTest {

    @Mock
    private lateinit var mockSettings: Settings
    private lateinit var urlManager: UrlManager

    @Before
    fun setUp() {
        urlManager = UrlManager(mockSettings)
    }

    @Test
    fun testHttpsUrl() {
        `when`(mockSettings.networkHostname).thenReturn("example.com")
        `when`(mockSettings.networkPort).thenReturn("3000")
        `when`(mockSettings.networkUseHttps).thenReturn(true)

        val expectedUrl = "https://example.com:3000"
        assertEquals(expectedUrl, urlManager.base)
    }

    @Test
    fun testHttpUrl() {
        `when`(mockSettings.networkHostname).thenReturn("example.com")
        `when`(mockSettings.networkPort).thenReturn("8000")
        `when`(mockSettings.networkUseHttps).thenReturn(false)

        val expectedUrl = "http://example.com:8000"
        assertEquals(expectedUrl, urlManager.base)
    }

    @Test
    fun testHttpUrlNoPort() {
        `when`(mockSettings.networkHostname).thenReturn("example.com")
        `when`(mockSettings.networkPort).thenReturn("")
        `when`(mockSettings.networkUseHttps).thenReturn(false)

        val expectedUrl = "http://example.com"
        assertEquals(expectedUrl, urlManager.base)
    }

    @Test
    fun testAuthenticationUrl() {
        `when`(mockSettings.networkUseHttps).thenReturn(true)
        `when`(mockSettings.networkHostname).thenReturn("example.com")
        `when`(mockSettings.networkPort).thenReturn("8000")

        val expectedUrl = "https://example.com:8000/api/user/auth"
        assertEquals(expectedUrl, urlManager.authenticationUrl)
    }

    @Test
    fun testPatientUrl() {
        `when`(mockSettings.networkUseHttps).thenReturn(true)
        `when`(mockSettings.networkHostname).thenReturn("example.com")
        `when`(mockSettings.networkPort).thenReturn("8000")

        val expectedUrl = "https://example.com:8000/api/patients"
        assertEquals(expectedUrl, urlManager.patientUrl)
    }

    @Test
    fun testReadingUrl() {
        `when`(mockSettings.networkUseHttps).thenReturn(true)
        `when`(mockSettings.networkHostname).thenReturn("example.com")
        `when`(mockSettings.networkPort).thenReturn("8000")

        val expectedUrl = "https://example.com:8000/api/readings"
        assertEquals(expectedUrl, urlManager.readingUrl)
    }
}
