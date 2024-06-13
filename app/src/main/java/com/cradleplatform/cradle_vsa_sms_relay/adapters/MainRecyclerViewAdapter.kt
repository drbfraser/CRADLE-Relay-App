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
@Suppress("TooManyFunctions")
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

    @Suppress("LongMethod")
    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        val smsRelayEntity: SmsRelayEntity = sms[position]
        val numFragmentsReceived = smsRelayEntity.numFragmentsReceived
        val totalFragmentsFromMobile = smsRelayEntity.totalFragmentsFromMobile
        val isServerError = smsRelayEntity.isServerError
        val isSentToServer = smsRelayEntity.isSentToServer

        holder.receivedDateTime.text = smsRelayEntity.getDateAndTime()
        holder.duration.text = ""
        holder.phone.text = smsRelayEntity.getPhoneNumber()

        // Reset visibility for all views to default states
        setImageViewsForDefault(holder)

        if (numFragmentsReceived <= totalFragmentsFromMobile) {
//            Log.d("look","this is id in adapter view ${smsRelayEntity.id}")
            holder.receivingMobile.text = "Receiving $numFragmentsReceived out of " +
                    "$totalFragmentsFromMobile messages"
            holder.loadingMark1.visibility = View.VISIBLE
            holder.imageView1.alpha = Companion.alphaDim
        }
        if (numFragmentsReceived == totalFragmentsFromMobile) {
            setImageViewsForMessagesReceivedFromMobile(holder)
        }
        if (isServerError == true) {
            holder.receivingMobile.text = "Something went wrong on server..."
            setImageViewsForServerError(holder)
        }
        // second conditional so that the text is not changed after server error
        if(isSentToServer && isServerError != true) {
            setImageViewsForWaitingServerResponse(holder)
        }
        if (smsRelayEntity.smsPacketsToMobile.isEmpty() && smsRelayEntity.isServerResponseReceived
            && smsRelayEntity.isServerError == false){
            setImageViewsForComplete(holder)
        }
        //when the user clicks cancel, key is expired but its never sent to the server
        if(smsRelayEntity.isKeyExpired && !isSentToServer){
            setImageViewsForKeyExpired(holder)
        }
    }

    private fun setImageViewsForServerError(holder: SMSViewHolder) {
        holder.imageView1.alpha = Companion.alphaDim
        holder.imageView2.alpha = Companion.alphaDim
        holder.imageView3.alpha = Companion.alphaDim
        holder.imageView4.alpha = Companion.alphaDim
        holder.checkMark3.visibility = View.INVISIBLE
        holder.checkMark4.visibility = View.INVISIBLE
        // we got the messages, which is why we are sending it to the server, hence check mark is
        // visible
        holder.checkMark1.visibility = View.VISIBLE
        holder.checkMark2.visibility = View.VISIBLE // same here - it was sent to the server fine
        holder.failedMark4.visibility = View.VISIBLE
        holder.failedMark3.visibility = View.VISIBLE
        holder.loadingMark3.visibility = View.INVISIBLE
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
        holder.loadingMark3.visibility = View.INVISIBLE
        holder.imageView1.alpha = Companion.alphaDim
        holder.imageView2.alpha = Companion.alphaDim
        holder.imageView3.alpha = Companion.alphaDim
        holder.imageView4.alpha = Companion.alphaDim
    }

    private fun setImageViewsForDefault(holder: SMSViewHolder) {
        holder.checkMark1.visibility = View.INVISIBLE
        holder.checkMark2.visibility = View.INVISIBLE
        holder.checkMark3.visibility = View.INVISIBLE
        holder.checkMark4.visibility = View.INVISIBLE
        holder.failedMark1.visibility = View.INVISIBLE
        holder.failedMark2.visibility = View.INVISIBLE
        holder.failedMark3.visibility = View.INVISIBLE
        holder.failedMark4.visibility = View.INVISIBLE
        holder.loadingMark1.visibility = View.INVISIBLE
        holder.loadingMark2.visibility = View.INVISIBLE
        holder.loadingMark3.visibility = View.INVISIBLE
        holder.loadingMark4.visibility = View.INVISIBLE
        holder.imageView1.alpha = Companion.alphaDefault
        holder.imageView2.alpha = Companion.alphaDefault
        holder.imageView3.alpha = Companion.alphaDefault
        holder.imageView4.alpha = Companion.alphaDefault
    }

    private fun setImageViewsForWaitingServerResponse(holder: SMSViewHolder) {
        holder.receivingMobile.text = "Waiting for response from the server.."
        holder.checkMark1.visibility = View.VISIBLE
        holder.checkMark2.visibility = View.VISIBLE
        holder.imageView1.alpha = Companion.alphaDim
        holder.imageView2.alpha = Companion.alphaDim
        holder.failedMark1.visibility = View.INVISIBLE
        holder.failedMark2.visibility = View.INVISIBLE
        holder.loadingMark2.visibility = View.INVISIBLE
        holder.loadingMark3.visibility = View.VISIBLE
        holder.imageView3.alpha = Companion.alphaDim
    }

    private fun setImageViewsForKeyExpired(holder: SMSViewHolder) {
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
        holder.loadingMark1.visibility = View.INVISIBLE
    }

    private fun setImageViewsForMessagesReceivedFromMobile(holder: SMSViewHolder) {
        holder.receivingMobile.text = "Received all messages. Forwarding it to the server"
        holder.checkMark1.visibility = View.VISIBLE
        holder.imageView1.alpha = Companion.alphaDim
        holder.failedMark1.visibility = View.INVISIBLE
        holder.loadingMark1.visibility = View.INVISIBLE
        holder.loadingMark2.visibility = View.VISIBLE
        holder.imageView2.alpha = Companion.alphaDim
    }


    inner class SMSViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val phone: TextView = itemView.findViewById<TextView>(R.id.phone)
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
        val loadingMark1: ImageView = itemView.findViewById(R.id.receivedMobileLoadingMark)
        val loadingMark2: ImageView = itemView.findViewById(R.id.UploadServerLoadingMark)
        val loadingMark3: ImageView = itemView.findViewById(R.id.receiveServerLoadingMark)
        val loadingMark4: ImageView = itemView.findViewById(R.id.sentMobileLoadingMark)

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
