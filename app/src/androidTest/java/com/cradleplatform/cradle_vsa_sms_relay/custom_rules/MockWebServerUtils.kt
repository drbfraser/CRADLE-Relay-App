package com.cradleplatform.cradle_vsa_sms_relay.custom_rules

import android.content.SharedPreferences
import com.cradleplatform.cradle_vsa_sms_relay.model.Settings
import com.cradleplatform.cradle_vsa_sms_relay.model.UrlManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import okhttp3.mockwebserver.MockWebServer
import java.net.URL

object MockWebServerUtils {
    fun createMockServer(sharedPreferences: SharedPreferences? = null, configBlock: MockWebServer.() -> Unit): MockWebServer {
        val mockServer = MockWebServer().apply(configBlock)

        val mockSharedPrefs = sharedPreferences ?: mockk()

        val mockSettings = mockk<Settings> {
            val mockServerUrl = URL(mockServer.url("/").toString())
            every { networkHostname } returns mockServerUrl.host
            every { networkPort } returns mockServerUrl.port.toString()
            every { networkUseHttps } returns false
        }

        val urlManager = UrlManager(mockSettings)
        // 如果需要在其他地方使用 urlManager，可以在这里配置

        return mockServer
    }
}
