package com.cradleplatform.cradle_vsa_sms_relay.views

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.cradleplatform.smsrelay.R
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradleplatform.smsrelay.utilities.DateTimeUtil
import org.json.JSONException
import org.json.JSONObject

class ReferralAlertDialog(context: Context, var smsReferralEntity: SmsReferralEntity) :
    AlertDialog(context) {

    private lateinit var sendToServiceButtonClickListener: View.OnClickListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.referral_alert_dialog)
        findViewById<TextView>(R.id.titleAD).text = smsReferralEntity.id
        val msg: String = try {
            JSONObject(smsReferralEntity.jsonData.toString()).toString(JSON_INDENT)
        } catch (e: JSONException) {
            smsReferralEntity.jsonData.toString()
        }
        findViewById<TextView>(R.id.jsonDataAd).setOnClickListener {
            (it as TextView).text = msg
        }
        findViewById<TextView>(R.id.timeReceivedAd).text =
            DateTimeUtil.convertUnixToTimeString(smsReferralEntity.timeReceived)
        findViewById<TextView>(R.id.numAttemptAd).text =
            smsReferralEntity.numberOfTriesUploaded.toString()
        findViewById<TextView>(R.id.refUploadedAd).text = smsReferralEntity.isUploaded.toString()
        findViewById<TextView>(R.id.errorDataAd).text = smsReferralEntity.errorMessage

        findViewById<Button>(R.id.sendToServerAdButton).setOnClickListener(
            sendToServiceButtonClickListener
        )
        findViewById<Button>(R.id.cancelAdButton).setOnClickListener { this.cancel() }
        // need to show cardview corners
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun setOnSendToServerListener(onClickListener: View.OnClickListener) {
        this.sendToServiceButtonClickListener = onClickListener
    }

    companion object {
        const val JSON_INDENT = 4
    }
}
