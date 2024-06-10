package com.cradleplatform.cradle_vsa_sms_relay

import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import org.junit.Assert.*
import org.junit.Test

class SmsRelayEntityTest {

    @Test
    fun testInitialization() {
        val smsRelayEntity = SmsRelayEntity(
            id = "12345",
            numFragmentsReceived = 0,
            totalFragmentsFromMobile = 5,
            smsPacketsFromMobile = mutableListOf(),
            timestampsDataMessagesReceived = mutableListOf(),
            timestampsDataMessagesSent = mutableListOf(),
            isServerResponseReceived = false,
            isServerError = null,
            errorMessage = null,
            smsPacketsToMobile = mutableListOf(),
            numFragmentsSentToMobile = null,
            totalFragmentsFromServer = null,
            numberOfTriesUploaded = 0,
            deliveryReportSent = false,
            isCompleted = false
        )

        assertEquals("12345", smsRelayEntity.id)
        assertEquals(0, smsRelayEntity.numFragmentsReceived)
        assertFalse(smsRelayEntity.isServerResponseReceived)
    }

    @Test
    fun testComparison() {
        val smsRelay1 = SmsRelayEntity(
            id = "12345",
            numFragmentsReceived = 1,
            totalFragmentsFromMobile = 3,
            smsPacketsFromMobile = mutableListOf("message1", "message2"),
            timeRequestInitiated = 1000L,
            timestampsDataMessagesReceived = mutableListOf(1000L, 1010L),
            timestampsDataMessagesSent = mutableListOf(1100L, 1110L),
            isServerResponseReceived = true,
            isServerError = false,
            errorMessage = null,
            smsPacketsToMobile = mutableListOf("response1"),
            numFragmentsSentToMobile = 1,
            totalFragmentsFromServer = 2,
            numberOfTriesUploaded = 1,
            deliveryReportSent = true,
            isCompleted = false
        )

        val smsRelay2 = SmsRelayEntity(
            id = "67890",
            numFragmentsReceived = 2,
            totalFragmentsFromMobile = 4,
            smsPacketsFromMobile = mutableListOf("message3", "message4"),
            timeRequestInitiated = 2000L,
            timestampsDataMessagesReceived = mutableListOf(2000L, 2010L),
            timestampsDataMessagesSent = mutableListOf(2100L, 2110L),
            isServerResponseReceived = false,
            isServerError = true,
            errorMessage = "Timeout",
            smsPacketsToMobile = mutableListOf("response2"),
            numFragmentsSentToMobile = 2,
            totalFragmentsFromServer = 3,
            numberOfTriesUploaded = 2,
            deliveryReportSent = false,
            isCompleted = true
        )

        assertTrue(smsRelay1 < smsRelay2)
    }

}
