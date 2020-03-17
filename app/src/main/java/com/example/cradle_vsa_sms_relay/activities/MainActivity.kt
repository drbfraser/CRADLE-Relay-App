package com.example.cradle_vsa_sms_relay.activities

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cradle_vsa_sms_relay.*
import com.example.cradle_vsa_sms_relay.broad_castrecivers.ServiceToActivityBroadCastReciever

class MainActivity : AppCompatActivity(),
    MessageListener {
    var smsList:ArrayList<Sms> = ArrayList();
    private var isServiceStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupStartService()
        setupStopService()

        registerReceiver(
            ServiceToActivityBroadCastReciever(
                this
            ), IntentFilter("update"))

    }


    private fun setupStopService() {
        findViewById<Button>(R.id.btnStopService).setOnClickListener{
            if(isServiceStarted) {
                val intent: Intent = Intent(this, SmsService::class.java)
                intent.setAction(SmsService.STOP_SERVICE)
                ContextCompat.startForegroundService(this, intent)
                isServiceStarted = false
            }
        }
    }

    private fun setupStartService() {
        findViewById<Button>(R.id.btnStartService).setOnClickListener{
            checkpermissions()
        }
    }


    private fun checkpermissions() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(this, arrayOf( Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.RECEIVE_MMS),99)
            } else{
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.RECEIVE_MMS),99)
            }
        } else{
            //permission already granted
            if (!isServiceStarted) {
                startService()
            }

        }
    }
    fun startService(){
        val serviceIntent = Intent(this,
            SmsService::class.java)
        serviceIntent.setAction(SmsService.START_SERVICE)
        ContextCompat.startForegroundService(this,serviceIntent)
        isServiceStarted = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==99){
            //do whatever when permissions are granted
            if (!isServiceStarted) {
                startService()

            }
        }
    }

    override fun messageRecieved(message: Sms) {
        var smsRecyclerView:RecyclerView = findViewById(R.id.messageRecyclerview)
        smsList.add(0,message)
        var adapter = SmsRecyclerViewAdaper(smsList)
        smsRecyclerView.adapter = adapter
        var layout: RecyclerView.LayoutManager = LinearLayoutManager(this)
        smsRecyclerView.layoutManager = layout
        adapter.notifyDataSetChanged()
    }
}
