package com.example.cradle_vsa_sms_relay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SmsRecyclerViewAdaper(smsList: List<Sms>) :
    RecyclerView.Adapter<SmsRecyclerViewAdaper.SMSViewHolder>() {
    private var sms:List<Sms> = smsList

    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var smsText: TextView
        init {
           smsText= itemView.findViewById<TextView>(R.id.txtBody)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMSViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.sms_recycler_item,parent, false)
        return SMSViewHolder(v)

    }

    override fun getItemCount(): Int {
        return sms.size
    }

    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        holder.smsText.text = sms.get(position).messageBody
    }
}