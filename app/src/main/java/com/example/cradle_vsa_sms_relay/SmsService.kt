package com.example.cradle_vsa_sms_relay

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SmsService : Service(), MessageListener{


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return -1
    }


    override fun messageRecieved(message: String, body: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }
}