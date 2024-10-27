package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestPhase
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestResult

/**
 * Adapter for updating the recycler view UI in main activity
 * to display the status of a SMS Relay transaction
 */
@Suppress("TooManyFunctions")
class MainRecyclerViewAdapter : RecyclerView.Adapter<MainRecyclerViewAdapter.SMSViewHolder>() {
    var sms: List<RelayRequest> = ArrayList()
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
    fun setRelayList(smsRelayEntities: List<RelayRequest>) {
        this.sms = smsRelayEntities
        phoneList.clear()
        phoneList.add("All")
        phoneList.addAll(smsRelayEntities.map { it.phoneNumber }.distinct())
        notifyDataSetChanged()
    }
    fun getPhoneNumbers(): List<String> {
        return phoneList
    }

    @Suppress("LongMethod")
    override fun onBindViewHolder(holder: SMSViewHolder, position: Int) {
        val relayRequest: RelayRequest = sms[position]
        val numPacketsReceived = relayRequest.dataPacketsFromMobile.count { it != null }
        val expectedNumPackets = relayRequest.expectedNumPackets
        val context = holder.itemView.context


        holder.receivedDateTime.text = relayRequest.getDateAndTime()
        holder.duration.text = ""
        holder.phone.text = relayRequest.phoneNumber

        // Reset visibility for all views to default states
        setImageViewsForDefault(holder)

        when (relayRequest.requestResult) {
            RelayRequestResult.OK -> {
                setImageViewsForComplete(holder, relayRequest.getDuration())
            }
            RelayRequestResult.ERROR -> {
                holder.receivingMobile.text = relayRequest.errorMessage ?: context.getString(R.string.error_something_went_wrong)
                setImageViewsForError(holder, relayRequest.requestPhase)
            }
            else -> {
                when (relayRequest.requestPhase) {
                    RelayRequestPhase.RECEIVING_FROM_MOBILE -> {
                        holder.receivingMobile.text = context.getString(
                            R.string.receiving_mobile_messages,
                            numPacketsReceived,
                            expectedNumPackets
                        )
                        holder.loadingMark1.visibility = View.VISIBLE
                        holder.imageView1.alpha = ALPHA_DIM
                    }

                    RelayRequestPhase.RELAYING_TO_SERVER -> setImageViewsForMessagesReceivedFromMobile(
                        holder
                    )

                    RelayRequestPhase.RECEIVING_FROM_SERVER -> {
                        holder.receivingMobile.text = context.getString(
                            R.string.awaiting_server_response
                        )
                        setImageViewsForWaitingServerResponse(
                            holder
                        )
                    }

                    RelayRequestPhase.RELAYING_TO_MOBILE -> {
                        holder.receivingMobile.text = context.getString(
                            R.string.sending_messages_to_mobile,
                            relayRequest.numPacketsSent(),
                            relayRequest.dataPacketsToMobile.size
                        )
                    }

                    else -> throw IllegalStateException(context.getString(R.string.exception_impossible_branch))
                }
            }
        }
    }

    private fun setImageViewsForError(holder: SMSViewHolder, phase: RelayRequestPhase) {
        when (phase) {
            RelayRequestPhase.RECEIVING_FROM_MOBILE -> {
                holder.failedMark1.visibility = View.VISIBLE
                holder.loadingMark1.visibility = View.INVISIBLE

            }
            RelayRequestPhase.RELAYING_TO_SERVER -> {
                holder.checkMark1.visibility = View.VISIBLE
                holder.failedMark2.visibility = View.VISIBLE
                holder.loadingMark2.visibility = View.INVISIBLE
            }
            RelayRequestPhase.RECEIVING_FROM_SERVER -> {
                holder.checkMark1.visibility = View.VISIBLE
                holder.checkMark2.visibility = View.VISIBLE
                holder.failedMark3.visibility = View.VISIBLE
                holder.loadingMark3.visibility = View.INVISIBLE
            }
            RelayRequestPhase.RELAYING_TO_MOBILE -> {
                holder.checkMark1.visibility = View.VISIBLE
                holder.checkMark2.visibility = View.VISIBLE
                holder.checkMark3.visibility = View.VISIBLE

                holder.failedMark4.visibility = View.VISIBLE
                holder.loadingMark4.visibility = View.INVISIBLE
            }
            else -> throw IllegalStateException(holder.itemView.context.getString(R.string.exception_impossible_branch))
        }
    }
    private fun setImageViewsForComplete(holder: SMSViewHolder, duration: String) {
        holder.receivingMobile.text = holder.itemView.context.getString(R.string.completed_in, duration)
        holder.checkMark4.visibility = View.VISIBLE
        holder.checkMark1.visibility = View.VISIBLE
        holder.checkMark2.visibility = View.VISIBLE
        holder.checkMark3.visibility = View.VISIBLE
        holder.failedMark1.visibility = View.INVISIBLE
        holder.failedMark2.visibility = View.INVISIBLE
        holder.failedMark3.visibility = View.INVISIBLE
        holder.failedMark4.visibility = View.INVISIBLE
        holder.loadingMark3.visibility = View.INVISIBLE
        holder.imageView1.alpha = ALPHA_DIM
        holder.imageView2.alpha = ALPHA_DIM
        holder.imageView3.alpha = ALPHA_DIM
        holder.imageView4.alpha = ALPHA_DIM
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
        holder.imageView1.alpha = ALPHA_DIM
        holder.imageView2.alpha = ALPHA_DIM
        holder.imageView3.alpha = ALPHA_DIM
        holder.imageView4.alpha = ALPHA_DIM
    }

    private fun setImageViewsForWaitingServerResponse(holder: SMSViewHolder) {
        holder.receivingMobile.text = holder.itemView.context.getString(R.string.awaiting_server_response)
        holder.checkMark1.visibility = View.VISIBLE
        holder.checkMark2.visibility = View.VISIBLE
        holder.imageView1.alpha = ALPHA_DIM
        holder.imageView2.alpha = ALPHA_DIM
        holder.failedMark1.visibility = View.INVISIBLE
        holder.failedMark2.visibility = View.INVISIBLE
        holder.loadingMark2.visibility = View.INVISIBLE
        holder.loadingMark3.visibility = View.VISIBLE
        holder.imageView3.alpha = ALPHA_DIM
    }

    private fun setImageViewsForRelayingToMobile(holder: SMSViewHolder) {
        setImageViewsForDefault(holder)
        holder.checkMark1.visibility = View.VISIBLE
        holder.checkMark2.visibility = View.VISIBLE
        holder.checkMark3.visibility = View.VISIBLE
        holder.loadingMark4.visibility = View.VISIBLE
    }

    private fun setImageViewsForMessagesReceivedFromMobile(holder: SMSViewHolder) {
        holder.receivingMobile.text = holder.itemView.context.getString(R.string.messages_received_forward_to_server)

        holder.checkMark1.visibility = View.VISIBLE
        holder.imageView1.alpha = ALPHA_DIM

        holder.failedMark1.visibility = View.INVISIBLE
        holder.loadingMark1.visibility = View.INVISIBLE
        holder.loadingMark2.visibility = View.VISIBLE
        holder.imageView2.alpha = ALPHA_DIM
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
        private const val ALPHA_DIM = 0.2F
    }
}
