package com.cradleplatform.cradle_vsa_sms_relay.type_converters

import junit.framework.TestCase.assertEquals
import org.junit.Test

class TimeStampListConverterTest {

    private val converter = TimeStampListConverter()

    @Test
    fun testFromString() {
        val json = "[123456789, 987654321]"
        val expected = listOf(123456789L, 987654321L)
        assertEquals(expected, converter.fromString(json))
    }

    @Test
    fun testFromList() {
        val list = listOf(123456789L, 987654321L)
        val expected = "[123456789,987654321]"
        assertEquals(expected, converter.fromList(list))
    }
}
