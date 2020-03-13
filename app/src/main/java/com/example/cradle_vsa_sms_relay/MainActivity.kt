package com.example.cradle_vsa_sms_relay

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupStartService()
        setupStopService()
    }

    private fun setupStopService() {
        findViewById<Button>(R.id.btnStartService).setOnClickListener{

        }
    }

    private fun setupStartService() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
