package com.example.cradle_vsa_sms_relay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy

class SmsRecyclerViewAdaper(smsList: List<SmsReferralEntitiy>) :
    RecyclerView.Adapter<SmsRecyclerViewAdaper.SMSViewHolder>() {

    private var sms:List<SmsReferralEntitiy> = smsList

    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var smsText: TextView = itemView.findViewById<TextView>(R.id.txtBody)
        var statusImg:ImageView = itemView.findViewById(R.id.msgStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMSViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.sms_recycler_item,parent, false)
        return SMSViewHolder(v)

    }

    override fun getItemCount(): Int {
        return sms.size
    }

    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        holder.smsText.text = sms.get(position).jsonData
        if (sms.get(position).isUploaded) {
            holder.statusImg.setImageResource(R.drawable.ic_check_black_24dp)
        } else {
            holder.statusImg.setImageResource(R.drawable.ic_thumb_down_black_24dp)

        }
    }
}