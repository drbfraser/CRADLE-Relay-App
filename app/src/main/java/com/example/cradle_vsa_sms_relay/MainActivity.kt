package com.example.cradle_vsa_sms_relay

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(),MessageListener  {
    var x =5
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupStartService()
        setupStopService()
        registerReceiver(ServiceToActivityBroadCastReciever(this), IntentFilter("update"))

    }


    private fun setupStopService() {
        findViewById<Button>(R.id.btnStopService).setOnClickListener{
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
            val serviceIntent = Intent(this,SmsService::class.java)
            serviceIntent.putExtra("inputExtra","Foreground Service Example in Android")
            ContextCompat.startForegroundService(this,serviceIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==99){
            //do whatever when permissions are granted

            val serviceIntent = Intent(this,SmsService::class.java)
            serviceIntent.putExtra("inputExtra","Foreground Service Example in Android")
            ContextCompat.startForegroundService(this,serviceIntent)
        }
    }

    override fun messageRecieved(message: Sms) {
        Toast.makeText(this,"activirtrtfvvvvvvvvvvvvvvvvvvvvvdfsdfm", Toast.LENGTH_SHORT).show()
    }
}
