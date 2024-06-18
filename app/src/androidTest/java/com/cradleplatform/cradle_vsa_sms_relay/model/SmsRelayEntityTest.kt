package com.cradleplatform.cradle_vsa_sms_relay.model

import com.cradleplatform.cradle_vsa_sms_relay.utilities.DateTimeUtil
import org.junit.Assert.*
import org.junit.Test

class SmsRelayEntityTest {

    @Test
    fun testInitialization() {
        val smsRelayEntity = SmsRelayEntity(
            id = "12-345",
            numFragmentsReceived = 1,
            totalFragmentsFromMobile = 5,
            smsPacketsFromMobile = mutableListOf("message1", "message2"),
            timeRequestInitiated = System.currentTimeMillis(),
            timestampsDataMessagesReceived = mutableListOf(100000L),
            timestampsDataMessagesSent = mutableListOf(200000L),
            isServerResponseReceived = false,
            isServerError = null,
            errorMessage = "timeout",
            smsPacketsToMobile = mutableListOf(),
            numFragmentsSentToMobile = null,
            totalFragmentsFromServer = null,
            numberOfTriesUploaded = 0,
            deliveryReportSent = false,
            isCompleted = false
        )
        assertEquals("12", smsRelayEntity.getPhoneNumber())
        assertEquals("345", smsRelayEntity.getRequestIdentifier())
        assertEquals(DateTimeUtil.convertUnixToTimeString(System.currentTimeMillis()), smsRelayEntity.getDateAndTime())
        assertEquals("1m 40s", smsRelayEntity.getDuration())
    }

    @Test
    fun testComparison() {
        val smsRelayEntity1 = SmsRelayEntity(
            id = "1",
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

        val smsRelayEntity2 = SmsRelayEntity(
            id = "2",
            numFragmentsReceived = 2,
            totalFragmentsFromMobile = 4,
            smsPacketsFromMobile = mutableListOf("message3", "message4"),
            timeRequestInitiated = 2000L,
            timestampsDataMessagesReceived = mutableListOf(2000L, 2010L),
            timestampsDataMessagesSent = mutableListOf(2100L, 2110L),
            isServerResponseReceived = false,
            isServerError = true,
            errorMessage = null,
            smsPacketsToMobile = mutableListOf("response2"),
            numFragmentsSentToMobile = 2,
            totalFragmentsFromServer = 3,
            numberOfTriesUploaded = 2,
            deliveryReportSent = false,
            isCompleted = true
        )
        assertEquals(1000, smsRelayEntity2.compareTo(smsRelayEntity1))
    }

}
