package com.cradleplatform.cradle_vsa_sms_relay

import org.junit.Test
import org.junit.Assert.*
import com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver.MessageReceiver


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val messageReceiver: MessageReceiver? = null
        assertEquals(4, 2 + 2)
    }
}