package com.cradleplatform.cradle_vsa_sms_relay.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.adapters.DetailsExpandableListAdapter
import com.cradleplatform.cradle_vsa_sms_relay.adapters.ExpandableListData
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.view_model.DetailsViewModel

class CardDetailsActivity : AppCompatActivity() {
    private var expandableListView: ExpandableListView? = null
    private var adapter: ExpandableListAdapter? = null
    private var titleList: List<String>? = null
    private lateinit var cardDetailsViewModel: DetailsViewModel
    private var relayEntity: LiveData<SmsRelayEntity>? = null
    private var relayEntitySplit: LiveData<SmsRelayEntity>? = null
    private lateinit var expandableListData: ExpandableListData



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)

        cardDetailsViewModel =
            ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(application)
            )[DetailsViewModel::class.java]

        val date: String? = intent.getStringExtra("date")
        val phoneNumber: String? = intent.getStringExtra("phoneNumber")
        val duration: String? = intent.getStringExtra("duration")
        val id: String? = intent.getStringExtra("id")
        val idSplit: String? = intent.getStringExtra("idSplit")

        if (id != null) {
            cardDetailsViewModel.getRelayEntity(id)?.observe(this){
                Log.d("look here", "im updating $it")
//                makeDataToDisplay(it)
                expandableListData = ExpandableListData(it)
                expandableListView = findViewById(R.id.detailsList)
                if (expandableListView != null) {
                    val listData = expandableListData.data
                    titleList = ArrayList(listData.keys)
                    adapter = DetailsExpandableListAdapter(this, titleList as ArrayList<String>, listData)
                    expandableListView!!.setAdapter(adapter)

                }
            }
        }


        // Find the back button by its ID
        val backButton: Button = findViewById(R.id.backButton)

        // Set OnClickListener for the back button
        backButton.setOnClickListener {
            // Call finish to close the current activity and return to the previous one
            finish()
        }



//        val textDate: TextView = findViewById(R.id.textDate)
//        val textPhoneNumber: TextView = findViewById(R.id.textPhoneNumber)
//        val durationText: TextView = findViewById(R.id.durationtext)



        // Retrieve other data if passed

//        textDate.text = "Date: $date"
//        textPhoneNumber.text = "Phone Number: $phoneNumber"
//        durationText.text = "Duration: $duration"
    }
}
