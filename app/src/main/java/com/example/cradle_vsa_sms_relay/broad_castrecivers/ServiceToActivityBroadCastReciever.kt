package com.example.cradle_vsa_sms_relay.broad_castrecivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cradle_vsa_sms_relay.SingleMessageListener
import com.example.cradle_vsa_sms_relay.Sms

/**
 * this broadcast receiver sends message to activity from service whenever service receives a sms
 */
open class ServiceToActivityBroadCastReciever(var mListener: SingleMessageListener? = null) : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 != null) {
            if (p1.action.equals("update")){
                val intent = p1.extras
                val message: String? = intent?.getString("sms");
                val status = intent?.getInt("status")
                //its in json format
                val sms = Sms.fromJson(message.toString())
                if (status != null) {
                    sms.status = status
                }
                //sending the sms to json
                mListener?.singleMessageRecieved(sms)
            }
        }
    }
}