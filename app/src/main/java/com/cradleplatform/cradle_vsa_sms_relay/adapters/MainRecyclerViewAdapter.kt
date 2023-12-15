package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.smsrelay.R

class MainRecyclerViewAdapter () :
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

    //TODO Update bind function to use UI prototype
    //TODO add onclicklistener for item
    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        val smsRelayEntity: SmsRelayEntity = sms[position]

        holder.phone.text = smsRelayEntity.getPhoneNumber()
        holder.reqConter.text = smsRelayEntity.getRequestIdentifier()

        if (smsRelayEntity.numFragmentsReceived == smsRelayEntity.totalFragmentsFromMobile){
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
        }
        else{
            holder.checkBox1.isChecked = false
            holder.checkBox2.isChecked = false
        }
        if (smsRelayEntity.isServerError == true || smsRelayEntity.isServerResponseReceived == true){
            holder.checkBox3.isChecked = true
        }
        else{
            holder.checkBox3.isChecked = false
        }
        if(smsRelayEntity.smsPackets.isEmpty() && smsRelayEntity.isServerResponseReceived){
            holder.checkBox4.isChecked = true
        }
        else{
            holder.checkBox4.isChecked = false
        }
    }

    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var checkBox1 = itemView.findViewById<CheckBox>(R.id.checkBox1)
        var checkBox2 = itemView.findViewById<CheckBox>(R.id.checkBox2)
        var checkBox3 = itemView.findViewById<CheckBox>(R.id.checkBox3)
        var checkBox4 = itemView.findViewById<CheckBox>(R.id.checkBox4)
        var phone = itemView.findViewById<TextView>(R.id.phone)
        var reqConter = itemView.findViewById<TextView>(R.id.requestCounter)
    }
}