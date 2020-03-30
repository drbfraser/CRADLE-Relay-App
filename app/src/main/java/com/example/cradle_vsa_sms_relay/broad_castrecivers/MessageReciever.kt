package com.example.cradle_vsa_sms_relay.broad_castrecivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.example.cradle_vsa_sms_relay.MessageListener

/**
 * detects messages receives
 */
class MessageReciever : BroadcastReceiver() {

    companion object {
        private var meListener: MessageListener? = null;

        fun bindListener(messageListener: MessageListener){
            meListener = messageListener
        }
        fun unbindListener(){
            meListener = null
        }
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val data = p1?.extras
        val pdus = data?.get("pdus") as Array<Any>
        val messages = HashMap<String?, String?>();

        for (element in pdus){
            val smsMessage = SmsMessage.createFromPdu(element as ByteArray?)
            Log.d("bugg", smsMessage.messageBody+ " len: "+ smsMessage.messageBody.length);

            if (messages.containsKey(smsMessage.originatingAddress)){
                val newMsg:String = smsMessage.messageBody;
                val oldMsg: String? = messages[smsMessage.originatingAddress]
                messages[smsMessage.originatingAddress] = oldMsg + newMsg
            } else {
                messages[smsMessage.originatingAddress] = smsMessage.messageBody
            }
        }
        meListener?.messageMapRecieved(
            messages
            );

    }

}