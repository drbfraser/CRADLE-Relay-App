package com.cradleplatform.cradle_vsa_sms_relay.activities

import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.View
import android.widget.Button
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.adapters.DetailsExpandableListAdapter
import com.cradleplatform.cradle_vsa_sms_relay.adapters.ExpandableListData
import com.cradleplatform.cradle_vsa_sms_relay.view_model.DetailsViewModel

class DetailsActivity : AppCompatActivity() {
    private var expandableListView: ExpandableListView? = null
    private var adapter: ExpandableListAdapter? = null
    private var titleList: List<String>? = null
    private lateinit var cardDetailsViewModel: DetailsViewModel
    private lateinit var expandableListData: ExpandableListData
    private val expandedStateMap = SparseBooleanArray()

    private lateinit var resendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)

        cardDetailsViewModel =
            ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(application)
            )[DetailsViewModel::class.java]

        val requestId = intent.getStringExtra("requestId")?.toInt()
        val phoneNumber = intent.getStringExtra("phoneNumber")!!

        val messageNoTextView = findViewById<TextView>(R.id.messageNoTextView)
        resendButton = findViewById(R.id.btn_resend)  // Initialize Resend Button

        if (requestId != null) {
            Log.d("DetailsActivity", "requestId is not null")
            messageNoTextView.text = getString(R.string.message_number, requestId)

            cardDetailsViewModel.getRelayEntity(requestId, phoneNumber)?.observe(this) { message ->
                expandableListData = ExpandableListData(message)
                expandableListView = findViewById(R.id.detailsList)

                if (expandableListView != null) {
                    val listData = expandableListData.data
                    titleList = ArrayList(listData.keys)
                    adapter = DetailsExpandableListAdapter(this, titleList as ArrayList<String>, listData)
                    expandableListView!!.setAdapter(adapter)

                    for (i in 0 until titleList!!.size) {
                        if (expandedStateMap[i]) {
                            expandableListView!!.expandGroup(i)
                        }
                    }

                    // Track expanded/collapsed state
                    expandableListView!!.setOnGroupExpandListener { groupPosition ->
                        expandedStateMap.put(groupPosition, true)
                    }

                    expandableListView!!.setOnGroupCollapseListener { groupPosition ->
                        expandedStateMap.put(groupPosition, false)
                    }

                    // 🔹 Check if the message failed and show the resend button
                    if (message.status == "FAILED") {
                        resendButton.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            Log.d("DetailsActivity", "requestId is null")
        }

        // Resend Button Click Listener
        resendButton.setOnClickListener {
            resendFailedMessage(requestId, phoneNumber)
        }

        // Back Button
        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    // 🔹 Function to Resend the Failed Message
    private fun resendFailedMessage(requestId: Int?, phoneNumber: String) {
        if (requestId == null) {
            Toast.makeText(this, "Invalid request ID", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement actual resend logic here (e.g., API call)
        Toast.makeText(this, "Resending message to $phoneNumber...", Toast.LENGTH_SHORT).show()

        // Example: Trigger API Call to Resend
        cardDetailsViewModel.resendMessage(requestId, phoneNumber).observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Message resent successfully!", Toast.LENGTH_SHORT).show()
                resendButton.visibility = View.GONE // Hide button after resending
            } else {
                Toast.makeText(this, "Failed to resend message. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
