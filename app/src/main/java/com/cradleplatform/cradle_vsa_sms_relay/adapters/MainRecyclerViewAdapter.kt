package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.util.Log
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
    var sms: List<SmsRelayEntity> = ArrayList()
    private var phoneList: MutableList<String> = ArrayList()

    // Define a click listener interface
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    // Member variable for the listener
    private var listener: OnItemClickListener? = null

    // Method to set the listener
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

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
        Log.d("look here", "inside bind view holder ${smsRelayEntity.id} ")
        val numFragmentsReceived = smsRelayEntity.numFragmentsReceived
        val totalFragmentsFromMobile = smsRelayEntity.totalFragmentsFromMobile
        val isServerError = smsRelayEntity.isServerError
        val isSentToServer = smsRelayEntity.isSentToServer

        holder.receivedDateTime.text = smsRelayEntity.getDateAndTime()
        holder.duration.text = ""
        holder.phone.text = smsRelayEntity.getPhoneNumber()

        // Reset visibility for all views to default states
        holder.checkMark1.visibility = View.INVISIBLE
        holder.checkMark2.visibility = View.INVISIBLE
        holder.failedMark1.visibility = View.INVISIBLE
        holder.failedMark2.visibility = View.INVISIBLE
        holder.failedMark3.visibility = View.INVISIBLE
        holder.failedMark4.visibility = View.INVISIBLE
        holder.imageView1.alpha = Companion.alphaDefault
        holder.imageView2.alpha = Companion.alphaDefault
        holder.imageView3.alpha = Companion.alphaDefault
        holder.imageView4.alpha = Companion.alphaDefault


        if (numFragmentsReceived <= totalFragmentsFromMobile) {
            holder.receivingMobile.text = "Receiving $numFragmentsReceived out of $totalFragmentsFromMobile messages"
        }
        if (numFragmentsReceived == totalFragmentsFromMobile) {
            Log.d("look here","in 2 ${smsRelayEntity.id}")
            holder.receivingMobile.text = "Received all messages. Forwarding it to the server"
            holder.checkMark1.visibility = View.VISIBLE
//            holder.checkMark2.visibility = View.VISIBLE
            holder.imageView1.alpha = Companion.alphaDim
//            holder.imageView2.alpha = Companion.alphaDim
            holder.failedMark1.visibility = View.INVISIBLE
//            holder.failedMark2.visibility = View.INVISIBLE

        }
        if (isServerError == true) {
            Log.d("look here","in 3 ${smsRelayEntity.id}")
            holder.receivingMobile.text = "Something went wrong on server..."
            setImageViewsForServerError(holder)
        }
        if(isSentToServer && isServerError != true) { // second conditional so that the text is not changed after server error
            Log.d("look here", "in 7 ${smsRelayEntity.id}")
            holder.receivingMobile.text = "Waiting for response from the server.."
            holder.checkMark1.visibility = View.VISIBLE
            holder.checkMark2.visibility = View.VISIBLE
            holder.imageView1.alpha = Companion.alphaDim
            holder.imageView2.alpha = Companion.alphaDim
            holder.failedMark1.visibility = View.INVISIBLE
            holder.failedMark2.visibility = View.INVISIBLE
        }
        if (smsRelayEntity.smsPacketsToMobile.isEmpty() && smsRelayEntity.isServerResponseReceived) {
            Log.d("look here","in 1 ${smsRelayEntity.id}")
//            holder.checkMark4.visibility = View.VISIBLE
//            holder.checkMark1.visibility = View.VISIBLE
//            holder.checkMark2.visibility = View.VISIBLE
//            holder.checkMark3.visibility = View.VISIBLE
//            holder.imageView1.alpha = Companion.alphaDim
//            holder.imageView2.alpha = Companion.alphaDim
//            holder.imageView3.alpha = Companion.alphaDim
//            holder.imageView4.alpha = Companion.alphaDim
//            // Hide failed marks if check marks are visible
//            holder.failedMark1.visibility = View.INVISIBLE
//            holder.failedMark2.visibility = View.INVISIBLE
//            holder.failedMark3.visibility = View.INVISIBLE
//            holder.failedMark4.visibility = View.INVISIBLE
            setImageViewsForComplete(holder)
        }
        if(smsRelayEntity.isKeyExpired){
            Log.d("look here","in 8 ${smsRelayEntity.id}")
            holder.receivingMobile.text = "Something went wrong with the mobile"
            holder.checkMark1.visibility = View.INVISIBLE
            holder.checkMark2.visibility = View.INVISIBLE
            holder.checkMark3.visibility = View.INVISIBLE
            holder.imageView1.alpha = Companion.alphaDim
            holder.imageView2.alpha = Companion.alphaDim
            holder.imageView3.alpha = Companion.alphaDim
            holder.imageView4.alpha = Companion.alphaDim
            holder.failedMark1.visibility = View.VISIBLE
            holder.failedMark2.visibility = View.VISIBLE
            holder.failedMark3.visibility = View.VISIBLE
            holder.failedMark4.visibility = View.VISIBLE
        }

//        if (smsRelayEntity.isServerError == true) {
//            Log.d("look here","in 4 ${smsRelayEntity.id}")
//            holder.receivingMobile.text = "Something went wrong on server..."
//        }

//        if (smsRelayEntity.isCompleted) {
//            Log.d("look here","in 5 ${smsRelayEntity.id}")
//            holder.duration.text = smsRelayEntity.getDuration()
//            setImageViewsForComplete(holder)
//
//        }
//        else{
//            Log.d("look here","in 6 ${smsRelayEntity.id}")
//            setImageViewsForNoneComplete(holder)
//        }
//        if(smsRelayEntity.isServerError && )
    }

    private fun setImageViewsForServerError(holder: SMSViewHolder) {
        holder.imageView1.alpha = Companion.alphaDim
        holder.imageView2.alpha = Companion.alphaDim
        holder.imageView3.alpha = Companion.alphaDim
        holder.imageView4.alpha = Companion.alphaDim

        holder.checkMark3.visibility = View.INVISIBLE // maybe should only change this
        holder.checkMark4.visibility = View.INVISIBLE // maybe should only change this

        holder.checkMark1.visibility = View.VISIBLE // we got the messages, which is why we are sending it tp the server, hence check mark is visible
        holder.checkMark2.visibility = View.VISIBLE //same here - it was sent to the server fine
//        holder.failedMark1.visibility = View.VISIBLE
//        holder.failedMark2.visibility = View.VISIBLE
         holder.failedMark4.visibility = View.VISIBLE
         holder.failedMark3.visibility = View.VISIBLE

    }
    private fun setImageViewsForComplete(holder: SMSViewHolder) {
        holder.receivingMobile.text = "Completed"
        holder.checkMark4.visibility = View.VISIBLE
        holder.checkMark1.visibility = View.VISIBLE
        holder.checkMark2.visibility = View.VISIBLE
        holder.checkMark3.visibility = View.VISIBLE
        holder.failedMark1.visibility = View.INVISIBLE
        holder.failedMark2.visibility = View.INVISIBLE
        holder.failedMark3.visibility = View.INVISIBLE
        holder.failedMark4.visibility = View.INVISIBLE
        holder.imageView1.alpha = Companion.alphaDim
        holder.imageView2.alpha = Companion.alphaDim
        holder.imageView3.alpha = Companion.alphaDim
        holder.imageView4.alpha = Companion.alphaDim
    }

    private fun setImageViewsForNoneComplete(holder: SMSViewHolder) {
        if(holder.checkMark1.visibility == View.INVISIBLE) { //it was invisible why?
            holder.failedMark1.visibility = View.VISIBLE
            holder.imageView1.alpha = Companion.alphaDim
        }
        if(holder.checkMark2.visibility == View.INVISIBLE) {
            holder.failedMark2.visibility = View.VISIBLE
            holder.imageView2.alpha = Companion.alphaDim
        }
        if(holder.checkMark3.visibility == View.INVISIBLE) {
            holder.failedMark3.visibility = View.VISIBLE
            holder.imageView3.alpha = Companion.alphaDim
        }
        if(holder.checkMark4.visibility == View.INVISIBLE) {
            holder.failedMark4.visibility = View.VISIBLE
            holder.imageView4.alpha = Companion.alphaDim
        }
    }

    inner class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
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
        init {
            // Set click listener for the item view
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            // Delegate click event to the listener
            listener?.onItemClick(adapterPosition)
        }
    }

    companion object {
        private const val alphaDim = 0.2F
        private const val alphaDefault = 1.0F

    }
}
