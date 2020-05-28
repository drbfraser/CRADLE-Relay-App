package com.cradle.cradle_vsa_sms_relay

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.cradle.cradle_vsa_sms_relay.activities.MainActivity
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.utilities.DateTimeUtil

class SmsRecyclerViewAdaper(smsList: List<SmsReferralEntitiy>, context:Context) :
    RecyclerView.Adapter<SmsRecyclerViewAdaper.SMSViewHolder>() {

    private val sms: List<SmsReferralEntitiy> = smsList
    private val context:Context = context
    val onCLickList:ArrayList<MainActivity.AdapterClicker> = ArrayList<MainActivity.AdapterClicker>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMSViewHolder {
        val v: View =
            LayoutInflater.from(parent.context).inflate(R.layout.sms_recycler_item_layout, parent, false)
        return SMSViewHolder(v)

    }


    override fun getItemCount(): Int {
        return sms.size
    }

    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        val smsReferralEntitiy: SmsReferralEntitiy = sms[position
        ]
        holder.smsText.text = smsReferralEntitiy.jsonData
        if (!smsReferralEntitiy.isUploaded) {
            holder.statusImg.setBackgroundResource(R.drawable.ic_check_circle_24dp)
            holder.statusTxt.setText("Success")
            holder.statusTxt.setTextColor(context.resources.getColor(R.color.green))
            holder.errorTxt.visibility = GONE
        } else {
            holder.statusImg.setBackgroundResource(R.drawable.ic_error_24dp)
            holder.statusTxt.setText("Error")
            holder.statusTxt.setTextColor(context.resources.getColor(R.color.redDown))
            holder.errorTxt.visibility = VISIBLE
            holder.errorTxt.text = smsReferralEntitiy.errorMessage
        }
        holder.receivedTimeTxt.text =
            DateTimeUtil.convertUnixToTimeString(smsReferralEntitiy.timeRecieved)
        holder.layout.setOnClickListener {
            onCLickList.forEach { f -> f.onClick(smsReferralEntitiy) }
        }
    }

    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var smsText: TextView = itemView.findViewById<TextView>(R.id.txtBody)
        var statusImg: ImageView = itemView.findViewById(R.id.msgStatus)
        var receivedTimeTxt: TextView = itemView.findViewById(R.id.timeReceivedTxt)
        var errorTxt: TextView = itemView.findViewById(R.id.errorMsgTxt)
        var layout:ConstraintLayout = itemView.findViewById(R.id.referralLayout)
        var statusTxt:TextView = itemView.findViewById(R.id.statusTxt)
    }
}