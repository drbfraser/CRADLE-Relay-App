package com.example.cradle_vsa_sms_relay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage

class MessageReciever : BroadcastReceiver() {
    private var meListener: MessageListener? = null;

    fun bindListener(messageListener: MessageListener){
        meListener = messageListener
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<*>

        for (i in 0 until data?.size()!!){
            val smsMessage = SmsMessage.createFromPdu(pdus[i] as ByteArray?)
            meListener?.messageRecieved(smsMessage);
        }

    }
}