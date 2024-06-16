package com.cradleplatform.cradle_vsa_sms_relay.type_converters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsListConverterTest {

    private val converter = SmsListConverter()

    @Test
    fun testFromString() {
        val json = """["message1", "message2", "message3"]"""
        val expected = mutableListOf("message1", "message2", "message3")
        assertEquals(expected, converter.fromString(json))
    }

    @Test
    fun testToString() {
        val list = mutableListOf("message1", "message2", "message3")
        val expected = """["message1","message2","message3"]"""
        assertEquals(expected, converter.toString(list))
    }

    @Test
    fun testNullInputFromString() {
        assertNull(converter.fromString(null))
    }

    @Test
    fun testNullInputToString() {
        assertEquals("null", converter.toString(null))
    }
}
