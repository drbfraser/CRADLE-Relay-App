package com.cradleplatform.cradle_vsa_sms_relay.custom_rules

import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class SetUpMockServerRule(private val url: String) : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
                val editor = sharedPref.edit()
                editor.putString("base_url", url)
                editor.apply()
                Log.d("MOCK_SERVER_RULE", "Mock Server URL set to: $url")

                base?.evaluate()
            }
        }
    }
}