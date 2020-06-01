package com.cradle.cradle_vsa_sms_relay.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cradle.cradle_vsa_sms_relay.R

/**
 * A very basic application to send sms (Required to be default sms handler)
 */
class NewMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

    }
}
