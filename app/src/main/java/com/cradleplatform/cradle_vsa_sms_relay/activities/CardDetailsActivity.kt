package com.cradleplatform.cradle_vsa_sms_relay.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import com.cradleplatform.cradle_vsa_sms_relay.adapters.ExpandableListData.data
import android.widget.TextView
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.adapters.DetailsExpandableListAdapter

class CardDetails : AppCompatActivity() {
    private var expandableListView: ExpandableListView? = null
    private var adapter: ExpandableListAdapter? = null
    private var titleList: List<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)

        // Find the back button by its ID
        val backButton: Button = findViewById(R.id.backButton)

        // Set OnClickListener for the back button
        backButton.setOnClickListener {
            // Call finish to close the current activity and return to the previous one
            finish()
        }

        val date: String? = intent.getStringExtra("date")
        val phoneNumber: String? = intent.getStringExtra("phoneNumber")
        val duration: String? = intent.getStringExtra("duration")

//        val textDate: TextView = findViewById(R.id.textDate)
//        val textPhoneNumber: TextView = findViewById(R.id.textPhoneNumber)
//        val durationText: TextView = findViewById(R.id.durationtext)
        expandableListView = findViewById(R.id.detailsList)
        if (expandableListView != null) {
            val listData = data
            titleList = ArrayList(listData.keys)
            adapter = DetailsExpandableListAdapter(this, titleList as ArrayList<String>, listData)
            expandableListView!!.setAdapter(adapter)
//            expandableListView!!.setOnGroupExpandListener { groupPosition ->
//                Toast.makeText(
//                    applicationContext,
//                    (titleList as ArrayList<String>)[groupPosition] + " List Expanded.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//            expandableListView!!.setOnGroupCollapseListener { groupPosition ->
//                Toast.makeText(
//                    applicationContext,
//                    (titleList as ArrayList<String>)[groupPosition] + " List Collapsed.",
//                    Toast.LENGTH_SHORT
//                ).show()

            }


        // Retrieve other data if passed

//        textDate.text = "Date: $date"
//        textPhoneNumber.text = "Phone Number: $phoneNumber"
//        durationText.text = "Duration: $duration"
    }
}
