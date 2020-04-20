package com.cradle.cradle_vsa_sms_relay.views

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy

class ReferralAlertDialog : Dialog {

    var  smsReferralEntitiy:SmsReferralEntitiy

    constructor(context: Context, smsReferralEntitiy: SmsReferralEntitiy) : super(context) {
        this.smsReferralEntitiy = smsReferralEntitiy
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.referral_alert_dialog);
        setTitle(smsReferralEntitiy.id)
        findViewById<TextView>(R.id.jsonDataAd).setText(smsReferralEntitiy.jsonData)
    }

}