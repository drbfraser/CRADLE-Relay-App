package com.cradleplatform.cradle_vsa_sms_relay

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
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

    @get:Rule
    var rules: RuleChain = RuleChain.outerRule(SetUpMockServerRule()).around(activityScenarioRule)

    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            GrantPermissionRule.grant(*permissions, android.Manifest.permission.FOREGROUND_SERVICE)
        } else {
            GrantPermissionRule.grant(*permissions, android.Manifest.permission.READ_PHONE_STATE)
        }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = getInstrumentation().targetContext
        assertEquals("com.cradleplatform.cradle_vsa_sms_relay", appContext.packageName)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testingNewMessage() {
        val pduHex = "00000b915155255155f400004240400024102b99b0586b280d1299c5160c0683c1602d180cd6a21583465aac1693e168b9a0d038cbc16e37d970889bd968b4613166241261c41a6e98bbd166355aac5814128d3959b166cc158d33220c47941173b919ce36bcd16435a29138c4dd6ac118ad462b0663359a8d58c4c170c3202d78bbd562b6189178bbd56c31a35138ab1985c35aae96abdd6a42"
        val pduByteArray = pduHex.hexToByteArray()
        val pduByteArray2D = arrayOf<Any>(pduByteArray)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:+15555215556")
            putExtra("pdus", pduByteArray2D)
        }

        val smsMessage = MessageReceiver(getApplicationContext(), CoroutineScope(Dispatchers.Default))
        smsMessage.onReceive(getApplicationContext(), intent)
        onView(withText("+15555215554")).check(matches(isDisplayed()))
    }
}

class SetUpMockServerRule : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val sharedPref =
                    PreferenceManager.getDefaultSharedPreferences(getInstrumentation().targetContext)
                val editor = sharedPref.edit()
                editor.putString("base_url", "lol")
                editor.apply()

                base?.evaluate()
            }
        }
    }
}
