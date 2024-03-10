package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.smsrelay.R


/**
 * Adapter for updating the recycler view UI in main activity
 * to display the status of a SMS Relay transaction
 */

class MainRecyclerViewAdapter :
RecyclerView.Adapter<MainRecyclerViewAdapter.SMSViewHolder>() {

     private var sms: List<SmsRelayEntity> = ArrayList()
     private var phoneList: MutableList<String> = ArrayList()
    init {
        // Add "ALL" to the initial phoneList
        phoneList.add("All")
    }

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
        phoneList.clear()
        phoneList.add("All")
        phoneList.addAll(smsRelayEntities.map { it.getPhoneNumber() }.distinct())
        notifyDataSetChanged()
    }

    fun getPhoneNumbers(): List<String> {
        return phoneList
    }

    // TODO Update bind function to use UI prototype
    // TODO add onclicklistener for item
    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        val smsRelayEntity: SmsRelayEntity = sms[position]

        holder.receivedDateTime.text = "Date"
        holder.phone.text = smsRelayEntity.getPhoneNumber()
        holder.reqConter.text = smsRelayEntity.getRequestIdentifier()
        holder.receiveMobile.visibility = View.VISIBLE


        if (smsRelayEntity.numFragmentsReceived == smsRelayEntity.totalFragmentsFromMobile) {
            //to be deleted:Start
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
            //end
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.imageView1.alpha = 0.2F
            holder.imageView2.alpha = 0.2F
            holder.uploadServer.visibility = View.VISIBLE
            holder.receiveMobile.visibility = View.INVISIBLE

        } else {
            //to be deleted:Start
            holder.checkBox1.isChecked = false
            holder.checkBox2.isChecked = false
            //end
            holder.checkMark1.visibility = View.INVISIBLE
            holder.checkMark2.visibility = View.INVISIBLE
            holder.imageView1.alpha = 1F
            holder.imageView2.alpha = 1F
        }
        if (smsRelayEntity.isServerError == true || smsRelayEntity.isServerResponseReceived) {
            //to be deleted:Start
            holder.checkBox3.isChecked = true
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
            //end
            holder.imageView1.alpha = 0.2F
            holder.imageView2.alpha = 0.2F
            holder.imageView3.alpha = 0.2F

            holder.uploadServer.visibility = View.INVISIBLE
            holder.receiveServer.visibility = View.VISIBLE

            holder.checkMark3.visibility = View.VISIBLE
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
        } else {
            holder.checkBox3.isChecked = false
            holder.checkMark3.visibility = View.INVISIBLE
            holder.imageView3.alpha = 1F
        }
        if (smsRelayEntity.isServerError == true) {
            holder.error.visibility = View.VISIBLE
        } else {
            holder.error.visibility = View.GONE
        }
        if (smsRelayEntity.smsPacketsToMobile.isEmpty() && smsRelayEntity.isServerResponseReceived) {
            //to be deleted:Start
            holder.checkBox4.isChecked = true
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
            holder.checkBox3.isChecked = true
            //end
            holder.checkMark4.visibility = View.VISIBLE
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.checkMark3.visibility = View.VISIBLE
            holder.imageView1.alpha = 0.2F
            holder.imageView2.alpha = 0.2F
            holder.imageView3.alpha = 0.2F
            holder.imageView4.alpha = 0.2F
            holder.receiveServer.visibility = View.INVISIBLE
            holder.receiveMobile.visibility = View.INVISIBLE
            holder.uploadServer.visibility = View.INVISIBLE
            holder.completedMessage.visibility = View.VISIBLE
        } else {
            holder.checkBox4.isChecked = false
            holder.checkMark4.visibility = View.INVISIBLE
            holder.completedMessage.visibility = View.INVISIBLE
            holder.imageView4.alpha = 1F
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
        val checkMark1: ImageView = itemView.findViewById(R.id.receivedMobileCheckMark)
        val checkMark2: ImageView = itemView.findViewById(R.id.UploadServerCheckMark)
        val checkMark3: ImageView = itemView.findViewById(R.id.receiveServerCheckMark)
        val checkMark4: ImageView = itemView.findViewById(R.id.sentMobileCheckMark)
        val imageView1: ImageView = itemView.findViewById(R.id.receivedMobile)
        val imageView2: ImageView = itemView.findViewById(R.id.UploadServer)
        val imageView3: ImageView = itemView.findViewById(R.id.receiveServer)
        val imageView4: ImageView = itemView.findViewById(R.id.sentMobile)
        val completedMessage: TextView = itemView.findViewById(R.id.completed)
        val receiveMobile: TextView = itemView.findViewById(R.id.receivingMobile)
        val uploadServer: TextView = itemView.findViewById(R.id.uploadingServer)
        val receiveServer: TextView = itemView.findViewById(R.id.receivingServer)
        val sendMobile: TextView = itemView.findViewById(R.id.sendingMobile)
        val receivedDateTime: TextView = itemView.findViewById<TextView>(R.id.receivedDateTime)
    }
}

