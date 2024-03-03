package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.R

/**
 * Adapter for updating the recycler view UI in main activity
 * to display the status of a SMS Relay transaction
 */

class MainRecyclerViewAdapter :
RecyclerView.Adapter<MainRecyclerViewAdapter.SMSViewHolder>() {

    private var sms: List<SmsRelayEntity> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMSViewHolder {
        val v: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.sms_recycler_item_layout, parent, false)
        return SMSViewHolder(v)
    }

    override fun getItemCount(): Int {
        return sms.size
    }

    fun setRelayList(smsRelayEntities: List<SmsRelayEntity>) {
        this.sms = smsRelayEntities
        notifyDataSetChanged()
    }

    // TODO Update bind function to use UI prototype
    // TODO add onclicklistener for item
    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        val smsRelayEntity: SmsRelayEntity = sms[position]

        holder.phone.text = smsRelayEntity.getPhoneNumber()
        holder.reqConter.text = smsRelayEntity.getRequestIdentifier()

        if (smsRelayEntity.numFragmentsReceived == smsRelayEntity.totalFragmentsFromMobile) {
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
        } else {
            holder.checkBox1.isChecked = false
            holder.checkBox2.isChecked = false
        }
        if (smsRelayEntity.isServerError == true || smsRelayEntity.isServerResponseReceived == true) {
            holder.checkBox3.isChecked = true
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
        } else {
            holder.checkBox3.isChecked = false
        }
        if (smsRelayEntity.isServerError == true) {
            holder.error.visibility = View.VISIBLE
        } else {
            holder.error.visibility = View.GONE
        }
        if (smsRelayEntity.smsPacketsToMobile.isEmpty() && smsRelayEntity.isServerResponseReceived) {
            holder.checkBox4.isChecked = true
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
            holder.checkBox3.isChecked = true
        } else {
            holder.checkBox4.isChecked = false
        }
    }

    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox1: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox2)
        val checkBox3: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox3)
        val checkBox4: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox4)
        val phone: TextView = itemView.findViewById<TextView>(R.id.phone)
        val reqConter: TextView = itemView.findViewById<TextView>(R.id.requestCounter)
        val error: TextView = itemView.findViewById(R.id.serverErrorText)
    }
}
