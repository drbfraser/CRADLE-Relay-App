package com.cradle.cradle_vsa_sms_relay.views

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import com.cradle.cradle_vsa_sms_relay.R

class StopServiceAlertDialog(val contxt: Context) : AlertDialog(contxt) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stop_service_dialog)
    }
}
