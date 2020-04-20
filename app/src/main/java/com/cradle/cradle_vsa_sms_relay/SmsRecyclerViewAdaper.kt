package com.cradle.cradle_vsa_sms_relay

import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.cradle.cradle_vsa_sms_relay.activities.MainActivity
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.utilities.DateTimeUtil

class SmsRecyclerViewAdaper(smsList: List<SmsReferralEntitiy>) :
    RecyclerView.Adapter<SmsRecyclerViewAdaper.SMSViewHolder>() {

    private var sms: List<SmsReferralEntitiy> = smsList
    val onCLickList:ArrayList<MainActivity.AdapterClicker> = ArrayList<MainActivity.AdapterClicker>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMSViewHolder {
        val v: View =
            LayoutInflater.from(parent.context).inflate(R.layout.sms_recycler_item, parent, false)
        return SMSViewHolder(v)

    }


    override fun getItemCount(): Int {
        return sms.size
    }

    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        val smsReferralEntitiy: SmsReferralEntitiy = sms[position
        ]
        holder.smsText.text = smsReferralEntitiy.jsonData
        if (smsReferralEntitiy.isUploaded) {
            holder.statusImg.setImageResource(R.drawable.ic_thumb_up_green_24dp)
        } else {
            holder.statusImg.setImageResource(R.drawable.ic_thumb_down_black_24dp)
        }
        holder.receivedTimeTxt.text =
            DateTimeUtil.convertUnixToTimeString(smsReferralEntitiy.timeRecieved)
        if (!smsReferralEntitiy.errorMessage.equals("")) {
            holder.errorTxt.text = smsReferralEntitiy.errorMessage
            holder.errorLayout.visibility = VISIBLE
        }
        holder.layout.setOnClickListener {
            onCLickList.forEach { f -> f.onClick(smsReferralEntitiy) }
        }
    }

    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var smsText: TextView = itemView.findViewById<TextView>(R.id.txtBody)
        var statusImg: ImageView = itemView.findViewById(R.id.msgStatus)
        var receivedTimeTxt: TextView = itemView.findViewById(R.id.timeReceivedTxt)
        var errorLayout: LinearLayout = itemView.findViewById(R.id.errorLayout)
        var errorTxt: TextView = itemView.findViewById(R.id.errorMsgTxt)
        var layout:ConstraintLayout = itemView.findViewById(R.id.referralLayout)
    }
}