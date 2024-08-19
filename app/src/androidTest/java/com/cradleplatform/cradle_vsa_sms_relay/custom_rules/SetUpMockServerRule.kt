package com.cradleplatform.cradle_vsa_sms_relay.custom_rules

import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import com.cradleplatform.cradle_vsa_sms_relay.R
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.InetAddress

class SetUpMockServerRule : TestRule {

    lateinit var mockWebServer: MockWebServer
        private set

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                // Retrieve the SharedPreferences
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPref.edit()

                // Set the desired values for testing
                editor.putString(context.getString(R.string.key_server_hostname), "localhost")
                editor.putString(context.getString(R.string.key_server_port), "8080")
                editor.putBoolean(context.getString(R.string.key_server_use_https), false)
                editor.apply()

                // Get hostname and port from SharedPreferences
                val hostname = sharedPref.getString(context.getString(R.string.key_server_hostname), "localhost")
                val port = sharedPref.getString(context.getString(R.string.key_server_port), "8080")?.toIntOrNull() ?: 8080

                // Initialize the MockWebServer with the hostname and port from SharedPreferences
                mockWebServer = MockWebServer()
                mockWebServer.start(InetAddress.getByName(hostname), port)

                val mockServerUrl = mockWebServer.url("/").toString()

                // Store the URL in SharedPreferences
                editor.putString("base_url", mockServerUrl)
                editor.apply()

                Log.d("MOCK_SERVER_RULE", "Mock Server URL set to: $mockServerUrl")

                try {
                    base?.evaluate()
                } finally {
                    mockWebServer.shutdown()
                }
            }
        }
    }
}
