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
    // Constants for alpha values
    private val ALPHA_FULL = 1F
    private val ALPHA_DIM = 0.2F

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
        val numFragmentsReceived = smsRelayEntity.numFragmentsReceived
        val totalFragmentsFromMobile = smsRelayEntity.totalFragmentsFromMobile

        if (numFragmentsReceived <= totalFragmentsFromMobile) {
            holder.receivingMobile.text = "Receiving $numFragmentsReceived out of $totalFragmentsFromMobile"
            if (numFragmentsReceived == totalFragmentsFromMobile) {
                holder.receivingMobile.text = "Received all messages"
                holder.checkMark1.visibility = View.VISIBLE
                holder.imageView1.alpha = ALPHA_DIM
            }
        }
        holder.receivedDateTime.text = smsRelayEntity.getDateAndTime()
        holder.duration.text = smsRelayEntity.getDuration()
        holder.phone.text = smsRelayEntity.getPhoneNumber()
//        holder.receiveMobile.visibility = View.VISIBLE


        if (smsRelayEntity.numFragmentsReceived == smsRelayEntity.totalFragmentsFromMobile) {
            //to be deleted:Start
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
            //end
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.imageView1.alpha = ALPHA_DIM
            holder.imageView2.alpha = ALPHA_DIM
//            holder.uploadServer.visibility = View.VISIBLE
//            holder.receiveMobile.visibility = View.INVISIBLE

        } else {
            //to be deleted:Start
            holder.checkBox1.isChecked = false
            holder.checkBox2.isChecked = false
            //end
            holder.checkMark1.visibility = View.INVISIBLE
            holder.checkMark2.visibility = View.INVISIBLE
            holder.imageView1.alpha = ALPHA_FULL
            holder.imageView2.alpha = ALPHA_FULL
        }
        if (smsRelayEntity.isServerError == true || smsRelayEntity.isServerResponseReceived == true) {
            //to be deleted:Start
            holder.checkBox3.isChecked = true
            holder.checkBox1.isChecked = true
            holder.checkBox2.isChecked = true
            //end
            holder.imageView1.alpha = ALPHA_DIM
            holder.imageView2.alpha = ALPHA_DIM
            holder.imageView3.alpha = ALPHA_DIM

//            holder.uploadServer.visibility = View.INVISIBLE
//            holder.receiveServer.visibility = View.VISIBLE

            holder.checkMark3.visibility = View.VISIBLE
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
        } else {
            holder.checkBox3.isChecked = false
            holder.checkMark3.visibility = View.INVISIBLE
            holder.imageView3.alpha = ALPHA_FULL
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
            //end
            holder.checkMark4.visibility = View.VISIBLE
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.checkMark3.visibility = View.VISIBLE
            holder.imageView1.alpha = ALPHA_DIM
            holder.imageView2.alpha = ALPHA_DIM
            holder.imageView3.alpha = ALPHA_DIM
            holder.imageView4.alpha = ALPHA_DIM
//            holder.receiveServer.visibility = View.INVISIBLE
//            holder.receiveMobile.visibility = View.INVISIBLE
//            holder.uploadServer.visibility = View.INVISIBLE
//            holder.completedMessage.visibility = View.VISIBLE
        } else {
            holder.checkBox4.isChecked = false
            holder.checkMark4.visibility = View.INVISIBLE
//            holder.completedMessage.visibility = View.INVISIBLE
            holder.imageView4.alpha = ALPHA_FULL
        }
    }

    private fun serverResponseReceived(holder: SMSViewHolder) {
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
        holder.imageView1.alpha = ALPHA_DIM
        holder.imageView2.alpha = ALPHA_DIM
        holder.imageView3.alpha = ALPHA_DIM
        holder.imageView4.alpha = ALPHA_DIM
//        holder.receiveServer.visibility = View.INVISIBLE
//        holder.receiveMobile.visibility = View.INVISIBLE
//        holder.uploadServer.visibility = View.INVISIBLE
//        holder.completedMessage.visibility = View.VISIBLE

    }
    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox1: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox2)
        val checkBox3: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox3)
        val checkBox4: CheckBox = itemView.findViewById<CheckBox>(R.id.checkBox4)
        val phone: TextView = itemView.findViewById<TextView>(R.id.phone)
        val error: TextView = itemView.findViewById(R.id.serverErrorText)
        val checkMark1: ImageView = itemView.findViewById(R.id.receivedMobileCheckMark)
        val checkMark2: ImageView = itemView.findViewById(R.id.UploadServerCheckMark)
        val checkMark3: ImageView = itemView.findViewById(R.id.receiveServerCheckMark)
        val checkMark4: ImageView = itemView.findViewById(R.id.sentMobileCheckMark)
        val failedMark1: ImageView = itemView.findViewById(R.id.receivedMobileFailedMark)
        val failedMark2: ImageView = itemView.findViewById(R.id.UploadServerFailedMark)
        val failedMark3: ImageView = itemView.findViewById(R.id.receiveServerFailedMark)
        val failedMark4: ImageView = itemView.findViewById(R.id.sentMobileFailedMark)
        val imageView1: ImageView = itemView.findViewById(R.id.receivedMobile)
        val imageView2: ImageView = itemView.findViewById(R.id.UploadServer)
        val imageView3: ImageView = itemView.findViewById(R.id.receiveServer)
        val imageView4: ImageView = itemView.findViewById(R.id.sentMobile)
//        val completedMessage: TextView = itemView.findViewById(R.id.completed)
//        val receiveMobile: TextView = itemView.findViewById(R.id.receivingMobile)
//        val uploadServer: TextView = itemView.findViewById(R.id.uploadingServer)
//        val receiveServer: TextView = itemView.findViewById(R.id.receivingServer)
        val receivingMobile: TextView = itemView.findViewById(R.id.sendingMobile)
        val receivedDateTime: TextView = itemView.findViewById<TextView>(R.id.receivedDateTime)
        val duration: TextView = itemView.findViewById<TextView>(R.id.duration)
    }
}

