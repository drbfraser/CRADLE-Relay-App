package com.example.cradle_vsa_sms_relay.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cradle_vsa_sms_relay.*
import com.example.cradle_vsa_sms_relay.broad_castrecivers.ServiceToActivityBroadCastReciever
import com.example.cradle_vsa_sms_relay.dagger.MyApp
import com.example.cradle_vsa_sms_relay.database.MyDatabase
import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
    SingleMessageListener {
    private var smsList:MutableList<SmsReferralEntitiy> = ArrayList();
    private var isServiceStarted = false
    @Inject
    lateinit var database: MyDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MyApp).component.inject(this)
        setupStartService()
        setupStopService()
        setuprecyclerview()
        //register reciever to listen for events from the service
        registerReceiver(
            ServiceToActivityBroadCastReciever(
                this
            ), IntentFilter("update"))

    }

    private fun setuprecyclerview() {

        val smsRecyclerView:RecyclerView = findViewById(R.id.messageRecyclerview)
        var referrals = database.daoAccess().getAllReferrals().sortedByDescending { it.timeRecieved }
        val adapter = SmsRecyclerViewAdaper(referrals)
        smsRecyclerView.adapter = adapter
        val layout: RecyclerView.LayoutManager = LinearLayoutManager(this)
        smsRecyclerView.layoutManager = layout
        adapter.notifyDataSetChanged()

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

    override fun newMessageReceived() {
       setuprecyclerview()
    }

}
