package com.cradle.cradle_vsa_sms_relay.views

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.utilities.DateTimeUtil
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Text

class ReferralAlertDialog : AlertDialog {

    var  smsReferralEntitiy:SmsReferralEntitiy

    constructor(context: Context, smsReferralEntitiy: SmsReferralEntitiy) : super(context) {
        this.smsReferralEntitiy = smsReferralEntitiy
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.referral_alert_dialog);
        findViewById<TextView>(R.id.titleAD).setText("ID: "+smsReferralEntitiy.id)
        var msg:String
        try {
            msg = JSONObject(smsReferralEntitiy.jsonData).toString(4)
        } catch (e: JSONException) {
            msg = smsReferralEntitiy.jsonData.toString()
        }
        findViewById<TextView>(R.id.jsonDataAd).setText(msg)

        findViewById<TextView>(R.id.errorDataAd).setText(smsReferralEntitiy.errorMessage)
        findViewById<Button>(R.id.referralAdButton).setOnClickListener { this.cancel() }

        findViewById<TextView>(R.id.timeRecievedAd).setText(DateTimeUtil.convertUnixToTimeString(smsReferralEntitiy.timeRecieved))
        findViewById<TextView>(R.id.numAttemptAd).setText(smsReferralEntitiy.numberOfTriesUploaded.toString())
        findViewById<TextView>(R.id.refUploadedAd).setText(smsReferralEntitiy.isUploaded.toString())


    }

}