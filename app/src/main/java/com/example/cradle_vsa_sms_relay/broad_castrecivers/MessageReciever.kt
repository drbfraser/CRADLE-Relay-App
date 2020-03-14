package com.example.cradle_vsa_sms_relay.broad_castrecivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.example.cradle_vsa_sms_relay.MessageListener
import com.example.cradle_vsa_sms_relay.Sms

/**
 * detects messages receives
 */
class MessageReciever : BroadcastReceiver() {

    companion object {
        private var meListener: MessageListener? = null;
        fun bindListener(messageListener: MessageListener){
            meListener = messageListener
        }
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<Any>

        for (element in pdus){
            val smsMessage = SmsMessage.createFromPdu(element as ByteArray?)
            Log.d("bugg","message in receiver: "+ smsMessage.messageBody);

            meListener?.messageRecieved(
                Sms(smsMessage)
            );
        }

    }

}