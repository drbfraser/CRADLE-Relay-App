package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity

/**
 * Adapter for updating the recycler view UI in main activity
 * to display the status of a SMS Relay transaction
 */

class MainRecyclerViewAdapter : RecyclerView.Adapter<MainRecyclerViewAdapter.SMSViewHolder>() {
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
        val numFragmentsReceived = smsRelayEntity.numFragmentsReceived
        val totalFragmentsFromMobile = smsRelayEntity.totalFragmentsFromMobile
        holder.receivedDateTime.text = smsRelayEntity.getDateAndTime()
        holder.duration.text = ""
        holder.phone.text = smsRelayEntity.getPhoneNumber()

        if (numFragmentsReceived <= totalFragmentsFromMobile) {
            holder.receivingMobile.text = "Receiving $numFragmentsReceived out of $totalFragmentsFromMobile messages"
        }
        if (numFragmentsReceived == totalFragmentsFromMobile) {
            holder.receivingMobile.text = "Received all messages"
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.imageView1.alpha = alphaDim
            holder.imageView2.alpha = alphaDim
        }
        if (smsRelayEntity.isServerError == true || smsRelayEntity.isServerResponseReceived == true) {
            holder.imageView1.alpha = alphaDim
            holder.imageView2.alpha = Companion.alphaDim
            holder.imageView3.alpha = Companion.alphaDim
            holder.checkMark3.visibility = View.VISIBLE
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
        }
        if (smsRelayEntity.smsPacketsToMobile.isEmpty() && smsRelayEntity.isServerResponseReceived) {
            holder.checkMark4.visibility = View.VISIBLE
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.checkMark3.visibility = View.VISIBLE
            holder.imageView1.alpha = alphaDim
            holder.imageView2.alpha = alphaDim
            holder.imageView3.alpha = alphaDim
            holder.imageView4.alpha = alphaDim
        }

        if (smsRelayEntity.isCompleted) {
            holder.duration.text = smsRelayEntity.getDuration()
            holder.receivingMobile.text = "Completed"
            holder.checkMark4.visibility = View.VISIBLE
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.checkMark3.visibility = View.VISIBLE
            holder.imageView1.alpha = alphaDim
            holder.imageView2.alpha = alphaDim
            holder.imageView3.alpha = alphaDim
            holder.imageView4.alpha = alphaDim
        }
    }

    class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val phone: TextView = itemView.findViewById<TextView>(R.id.phone)

        // val error: TextView = itemView.findViewById(R.id.serverErrorText)
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
        val receivingMobile: TextView = itemView.findViewById(R.id.sendingMobile)
        val receivedDateTime: TextView = itemView.findViewById<TextView>(R.id.receivedDateTime)
        val duration: TextView = itemView.findViewById<TextView>(R.id.duration)
    }

    companion object {
        private const val alphaDim = 0.2F
        private const val alphaFull = 1F
    }
}
