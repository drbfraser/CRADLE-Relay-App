package com.example.cradle_vsa_sms_relay

import android.telephony.SmsMessage

public interface MessageListener {

    fun messageRecieved(message: SmsMessage)
}