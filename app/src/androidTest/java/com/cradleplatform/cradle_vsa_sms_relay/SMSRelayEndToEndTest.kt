package com.cradleplatform.cradle_vsa_sms_relay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import com.cradleplatform.cradle_vsa_sms_relay.activities.MainActivity
import com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver.MessageReceiver
import com.cradleplatform.cradle_vsa_sms_relay.custom_rules.SetUpMockServerRule
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SMSRelayEndToEndTest {

    private val permissions = arrayOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.INTERNET
    )
    private var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    private lateinit var webServer: MockWebServer
    private val setUpMockServerRule = SetUpMockServerRule()

    // This rule chain ensures that the mock sever URL is set the same as settings use the shared preferences
    // the activity is launched
    @get:Rule
    var rules: RuleChain = RuleChain.outerRule(setUpMockServerRule).around(activityScenarioRule)

    // set up mock server
    @Before
    fun setUp() {
        webServer = setUpMockServerRule.mockWebServer
    }


    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            GrantPermissionRule.grant(*permissions, android.Manifest.permission.FOREGROUND_SERVICE)
        } else {
            GrantPermissionRule.grant(*permissions, android.Manifest.permission.READ_PHONE_STATE)
        }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        webServer.shutdown()
    }

    @Test
    fun useAppContext() {
        val appContext = getInstrumentation().targetContext
        assertEquals("com.cradleplatform.cradle_vsa_sms_relay", appContext.packageName)
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun simpleEndToEndTest() = runTest {

        val mockServerUrl = webServer.url("/").toString()
        // Arrange
        Log.d("TEST", "Starting simpleEndToEndTest")
        Log.d("TEST", "Mock Server URL: $mockServerUrl")


        // Arrange
        val pduHex = "00000b915155255155f400004240901030812b99b0586b280d1299c5160c0683c1602d182cd6" +
                "1a066fb79a301824d268c4983137b31567b2dc8c48ac0d654259d13883e58a311c2d67acd96a34e22" +
                "c1884e570b7180e46b4116d33197146c3e58c37628e78cbc18631a18c988b156bb21a91069bd56e44" +
                "626e3814e28c445c2d3813c68239582c188c1967b8dbed161bc270c5616c689bc18a43"
        val pduByteArray = pduHex.hexToByteArray()
        val pduByteArray2D = arrayOf<Any>(pduByteArray)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:+15555215556")
            putExtra("pdus", pduByteArray2D)
        }

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"body\": \"26A80CC9E5A8E8E1C6BE794A8A41CB195af7ed643e3e4a1190170c51e3c056ce8dae2223c292bf53b7f91c59df47a388\", \"code\": 201}")
        webServer.enqueue(mockResponse)

        val smsMessage = MessageReceiver(getApplicationContext(), TestScope())

        // Act
        Log.d("TEST", "Sending intent to MessageReceiver")
        smsMessage.onReceive(getApplicationContext(), intent)

        // Assert with timeout
        val request1: RecordedRequest? = webServer.takeRequest(10, TimeUnit.SECONDS)
        if (request1 != null) {
            Log.d("TEST_REQUEST", "Request path: ${request1.path}")
            assertEquals("/api/sms_relay", request1.path)
            Log.d("TEST_REQUEST", "Request body: ${request1.body.readUtf8()}")
        } else {
            Log.e("TEST_REQUEST", "No request received within the timeout period")
        }

        @Test
        fun testSendAckMessage() = runTest {
            Log.d("TEST", "Starting testSendAckMessage")

            // Arrange
            val phoneNumber = "+15555215556"
            val ackMessage = "ACK"
            val requestIdentifier = "000000"
            val id = "$phoneNumber-$requestIdentifier"
            val relayEntity = SmsRelayEntity(id, 1, 1, mutableListOf(ackMessage), System.currentTimeMillis(), mutableListOf(), mutableListOf(), false, false, null, mutableListOf(), null, null, 1, false, false)

            // Set up MockWebServer response
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"body\": \"ACK_RECEIVED\", \"code\": 200}")
            webServer.enqueue(mockResponse)

            // Simulate receiving an ACK message
            val pduHex = "00000b915155255155f400004240901030812b99b0586b280d1299c5160c0683c1602d182cd6" // This should be the actual PDU hex string of an ACK message
            val pduByteArray = pduHex.hexToByteArray()
            val pduByteArray2D = arrayOf<Any>(pduByteArray)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("pdus", pduByteArray2D)
            }

            // Act
            Log.d("TEST", "Sending ACK intent to MessageReceiver")
            smsMessage.onReceive(getApplicationContext(), intent)

            // Assert
            val request1: RecordedRequest? = webServer.takeRequest(10, TimeUnit.SECONDS)
            if (request1 != null) {
                Log.d("TEST_REQUEST", "Request path: ${request1.path}")
                assertEquals("/api/sms_relay", request1.path)
                Log.d("TEST_REQUEST", "Request body: ${request1.body.readUtf8()}")
            } else {
                Log.e("TEST_REQUEST", "No request received within the timeout period")
            }
        }

        testSendAckMessage()

        @Test
        fun testSendDataToServer() = runTest {
            Log.d("TEST", "Starting testSendDataToServer")

            // Arrange
            val phoneNumber = "+15555215556"
            val restMessage = "REST"
            val requestIdentifier = "000002"
            val id = "$phoneNumber-$requestIdentifier"
            val relayEntity = SmsRelayEntity(id, 1, 1, mutableListOf(restMessage), System.currentTimeMillis(), mutableListOf(), mutableListOf(), false, false, null, mutableListOf(), null, null, 1, false, false)

            // Set up MockWebServer response
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"body\": \"REST_RECEIVED\", \"code\": 200}")
            webServer.enqueue(mockResponse)

            // Simulate receiving a rest message
            val pduHex = "00000b915155255155f400004240901030812b99b0586b280d1299c5160c0683c1602d182cd6" // This should be the actual PDU hex string of a rest message
            val pduByteArray = pduHex.hexToByteArray()
            val pduByteArray2D = arrayOf<Any>(pduByteArray)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("pdus", pduByteArray2D)
            }

            // Act
            Log.d("TEST", "Sending rest intent to MessageReceiver")
            smsMessage.onReceive(getApplicationContext(), intent)

            // Assert
            val request1: RecordedRequest? = webServer.takeRequest(10, TimeUnit.SECONDS)
            if (request1 != null) {
                Log.d("TEST_REQUEST", "Request path: ${request1.path}")
                assertEquals("/api/sms_relay", request1.path)
                Log.d("TEST_REQUEST", "Request body: ${request1.body.readUtf8()}")
            } else {
                Log.e("TEST_REQUEST", "No request received within the timeout period")
            }
        }

        testSendDataToServer()

        // Waiting to make sure all tasks are complete before moving on
        Log.d("TEST", "Waiting for tasks to complete")
        advanceUntilIdle()


        Log.d("TEST", "Checking if messages are displayed")
        onView(withText("+15555215554")).check(matches(isDisplayed()))
        onView(withText("Received all messages")).check(matches(isDisplayed()))

        Log.d("TEST", "Test completed")
    }

    // Utility method to convert a hex string to a ByteArray
    private fun String.hexToByteArray(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
