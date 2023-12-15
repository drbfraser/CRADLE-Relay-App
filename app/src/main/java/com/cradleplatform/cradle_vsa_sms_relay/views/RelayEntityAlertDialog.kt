package com.cradleplatform.cradle_vsa_sms_relay.views

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity

/**
 * dialog that pops up when a item is clicked on in the main activity recycler view
 */

class RelayEntityAlertDialog(context: Context, var smsRelayEntity: SmsRelayEntity):
    AlertDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO implement dialog box logic here
    }
}