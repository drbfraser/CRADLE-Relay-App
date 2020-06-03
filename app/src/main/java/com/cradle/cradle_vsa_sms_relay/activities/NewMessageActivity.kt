package com.cradle.cradle_vsa_sms_relay.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.cradle.cradle_vsa_sms_relay.R

/**
 * A very basic application to send sms (Required to be default sms handler)
 */
class NewMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)
        setupTextMSg();
    }

    private fun setupTextMSg() {
        val phonetxt = findViewById<EditText>(R.id.phoneNumber)
        val messageTxt = findViewById<EditText>(R.id.txtMessage)

        val sendButton = findViewById<Button>(R.id.sendButton)

        sendButton.setOnClickListener {
            if (phonetxt.text.toString() == ""){
                Toast.makeText(this,"No phone number",Toast.LENGTH_SHORT).show()
            } else if (messageTxt.text.toString() == ""){
                Toast.makeText(this,"There is no message to send",Toast.LENGTH_SHORT).show()
            } else {
                val smsManager = SmsManager.getDefault()
                smsManager.sendMultipartTextMessage(phonetxt.text.toString(),null,
                    smsManager.divideMessage(messageTxt.text.toString()),
                    null,null)
                phonetxt.setText("")
                messageTxt.setText("")
            }
        }


    }
}
