package com.example.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cradle_vsa_sms_relay.SingleMessageListener

/**
 * this broadcast receiver sends message to activity from service whenever service receives a sms
 */
open class ServiceToActivityBroadCastReciever(var mListener: SingleMessageListener? = null) :
    BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 != null) {
            if (p1.action.equals("update")) {
                //letting whoever is listening know we have received a new message update
                mListener?.newMessageReceived()
            }
        }
    }
}