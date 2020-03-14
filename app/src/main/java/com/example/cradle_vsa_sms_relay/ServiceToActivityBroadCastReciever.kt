package com.example.cradle_vsa_sms_relay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * this broadcast receiver sends message to activity from service whenever service receives a sms
 */
open class ServiceToActivityBroadCastReciever(var mListener: MessageListener? = null) : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 != null) {
            if (p1.action.equals("update")){
                Log.d("bugg","received from service")
                var intent = p1.extras
                var message: String? = intent?.getString("sms");
                val sms = Sms(message)

                mListener?.messageRecieved(sms)
            }
        }
    }
}