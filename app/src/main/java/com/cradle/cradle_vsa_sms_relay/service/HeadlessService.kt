package com.cradle.cradle_vsa_sms_relay.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class HeadlessService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}