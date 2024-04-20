package com.cradleplatform.cradle_vsa_sms_relay.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.cradleplatform.cradle_vsa_sms_relay.R

class CardDetails : AppCompatActivity() {
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

        val textDate: TextView = findViewById(R.id.textDate)
        val textPhoneNumber: TextView = findViewById(R.id.textPhoneNumber)
        val durationText: TextView = findViewById(R.id.durationtext)


        val date: String? = intent.getStringExtra("date")
        val phoneNumber: String? = intent.getStringExtra("phoneNumber")
        val duration: String? = intent.getStringExtra("duration")
        // Retrieve other data if passed

        textDate.text = "Date: $date"
        textPhoneNumber.text = "Phone Number: $phoneNumber"
        durationText.text = "Duration: $duration"
    }
}
