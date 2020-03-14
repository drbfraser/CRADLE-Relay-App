package com.example.cradle_vsa_sms_relay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log

class MessageReciever : BroadcastReceiver() {

    companion object {
        private var meListener: MessageListener? = null;
        fun bindListener(messageListener: MessageListener){
            Companion.meListener = messageListener
        }
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<Any>

        for (element in pdus){
            val smsMessage = SmsMessage.createFromPdu(element as ByteArray?)
            Log.d("bugg","message in receiver: "+ smsMessage.messageBody);

            Companion.meListener?.messageRecieved(Sms(smsMessage));
        }

    }

}