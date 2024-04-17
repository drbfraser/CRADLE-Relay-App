package com.cradleplatform.cradle_vsa_sms_relay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private val permissions = arrayOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.INTERNET
    )
    private var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    private var webServer: MockWebServer = MockWebServer()

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
    fun testingNewMessage() = runTest {
        val pduHex = "00000b915155255155f400004240901030812b99b0586b280d1299c5160c0683c1602d182cd6"+
                "1a066fb79a301824d268c4983137b31567b2dc8c48ac0d654259d13883e58a311c2d67acd96a34e22"+
                "c1884e570b7180e46b4116d33197146c3e58c37628e78cbc18631a18c988b156bb21a91069bd56e44"+
                "626e3814e28c445c2d3813c68239582c188c1967b8dbed161bc270c5616c689bc18a43"
        val pduByteArray = pduHex.hexToByteArray()
        val pduByteArray2D = arrayOf<Any>(pduByteArray)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:+15555215556")
            putExtra("pdus", pduByteArray2D)
        }

        val mockResponse = MockResponse()
            .setResponseCode(201)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"body\": \"26A80CC9E5A8E8E1C6BE794A8A41CB195af7ed643e3e4a1190170c51e3c056ce8dae2223c292bf53b7f91c59df47a388\", \"code\": 201}")

        webServer.enqueue(mockResponse)

        val smsMessage = MessageReceiver(getApplicationContext(), TestScope())
        smsMessage.onReceive(getApplicationContext(), intent)

        val request1: RecordedRequest = webServer.takeRequest()
        assertEquals("/api/sms_relay", request1.path)
        assertNotNull(request1.getHeader("Authorization"))

        advanceUntilIdle()
        onView(withText("+15555215554")).check(matches(isDisplayed()))
        onView(withText("Received all messages")).check(matches(isDisplayed()))
    }
}

class SetUpMockServerRule(private val url:String) : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                Log.d("url", url)
                val sharedPref =
                    PreferenceManager.getDefaultSharedPreferences(getInstrumentation().targetContext)
                val editor = sharedPref.edit()
                editor.putString("base_url", url)
                editor.apply()

                base?.evaluate()
            }
        }
    }
}
