package com.cradleplatform.cradle_vsa_sms_relay

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SimpleEndToEndTest {

    private val permissions = arrayOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.INTERNET
    )
    private var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    private var webServer: MockWebServer = MockWebServer()

    // This rule chain ensures that the mock sever URL is set in the shared preferences before
    // the activity is launched
    @get:Rule
    var rules: RuleChain = RuleChain.outerRule(SetUpMockServerRule(webServer.url("/").toString())).around(activityScenarioRule)

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
        // Context of the app under test.
        val appContext = getInstrumentation().targetContext
        assertEquals("com.cradleplatform.cradle_vsa_sms_relay", appContext.packageName)
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun simpleEndToEndTest() = runTest {
        // Arrange

        // This PDU was obtained by running SMS Relay and setting a breakpoint in the onReceive of
        // the MessageReceiver class, and sending a message to the app while its running.
        // You can find the contents of the PDU on this website https://twit88.com/home/utility/sms-pdu-encode-decode
        val pduHex = "00000b915155255155f400004240901030812b99b0586b280d1299c5160c0683c1602d182cd6"+
                "1a066fb79a301824d268c4983137b31567b2dc8c48ac0d654259d13883e58a311c2d67acd96a34e22"+
                "c1884e570b7180e46b4116d33197146c3e58c37628e78cbc18631a18c988b156bb21a91069bd56e44"+
                "626e3814e28c445c2d3813c68239582c188c1967b8dbed161bc270c5616c689bc18a43"
        val pduByteArray = pduHex.hexToByteArray()
        val pduByteArray2D = arrayOf<Any>(pduByteArray)

        // Intent that replicates what an incoming message would look like
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:+15555215556")
            putExtra("pdus", pduByteArray2D)
        }

        // Setting up a mock response for the mock server. The contents of the body do not matter
        // because SMS Relay does not care about it. I just causes an error when it is empty
        val mockResponse = MockResponse()
            .setResponseCode(201)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"body\": \"26A80CC9E5A8E8E1C6BE794A8A41CB195af7ed643e3e4a1190170c51e3c056ce8dae2223c292bf53b7f91c59df47a388\", \"code\": 201}")

        webServer.enqueue(mockResponse)

        val smsMessage = MessageReceiver(getApplicationContext(), TestScope())

        // Act
        smsMessage.onReceive(getApplicationContext(), intent)

        // Assert
        val request1: RecordedRequest = webServer.takeRequest()
        assertEquals("/api/sms_relay", request1.path)

        // Waiting to make sure all tasks are complete before moving on
        advanceUntilIdle()
        onView(withText("+15555215554")).check(matches(isDisplayed()))
        onView(withText("Received all messages")).check(matches(isDisplayed()))
    }
}
