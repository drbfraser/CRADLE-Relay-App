package com.example.cradle_vsa_sms_relay

public interface MessageListener {

    fun messageRecieved(message: String, body: String)
}