package com.cradle.cradle_vsa_sms_relay.views

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.utilities.DateTimeUtil
import org.json.JSONException
import org.json.JSONObject

class ReferralAlertDialog(context: Context, var smsReferralEntitiy: SmsReferralEntitiy) :
    AlertDialog(context) {

    private lateinit var sendToServiceButtonClickListener: View.OnClickListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.referral_alert_dialog);
        findViewById<TextView>(R.id.titleAD).setText(smsReferralEntitiy.id)
        val msg:String = try {
            JSONObject(smsReferralEntitiy.jsonData).toString(4)
        } catch (e: JSONException) {
            smsReferralEntitiy.jsonData.toString()
        }
        findViewById<TextView>(R.id.jsonDataAd).setText(msg)
        findViewById<TextView>(R.id.timeRecievedAd).setText(DateTimeUtil.convertUnixToTimeString(smsReferralEntitiy.timeRecieved))
        findViewById<TextView>(R.id.numAttemptAd).setText(smsReferralEntitiy.numberOfTriesUploaded.toString())
        findViewById<TextView>(R.id.refUploadedAd).setText(smsReferralEntitiy.isUploaded.toString())
        findViewById<TextView>(R.id.errorDataAd).setText(smsReferralEntitiy.errorMessage)

        findViewById<Button>(R.id.sendToServerAdButton).setOnClickListener(sendToServiceButtonClickListener)
        findViewById<Button>(R.id.cancelAdButton).setOnClickListener { this.cancel() }
        //need to show cardview corners
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun setOnSendToServerListener(onClickListener: View.OnClickListener){
        this.sendToServiceButtonClickListener = onClickListener
    }
}