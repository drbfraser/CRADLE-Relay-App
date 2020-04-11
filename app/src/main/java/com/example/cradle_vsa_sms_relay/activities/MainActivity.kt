package com.example.cradle_vsa_sms_relay.activities

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cradle_vsa_sms_relay.R
import com.example.cradle_vsa_sms_relay.SingleMessageListener
import com.example.cradle_vsa_sms_relay.SmsRecyclerViewAdaper
import com.example.cradle_vsa_sms_relay.SmsService
import com.example.cradle_vsa_sms_relay.broadcast_receiver.ServiceToActivityBroadCastReciever
import com.example.cradle_vsa_sms_relay.dagger.MyApp
import com.example.cradle_vsa_sms_relay.database.ReferralDatabase
import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
    SingleMessageListener {

    private var isServiceStarted = false
    @Inject
    lateinit var database: ReferralDatabase
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    lateinit var serviceToActivityBroadCastReciever: ServiceToActivityBroadCastReciever

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MyApp).component.inject(this)
        setupStartService()
        setupStopService()
        setuprecyclerview()
        //register reciever to listen for events from the service
        serviceToActivityBroadCastReciever = ServiceToActivityBroadCastReciever(this)
        registerReceiver(
            serviceToActivityBroadCastReciever, IntentFilter("update")
        )

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settingMenu -> {
                startActivity(Intent(this,SettingsActivity::class.java))
                true
            }else -> super.onOptionsItemSelected(item)
        }

        }

    private fun setuprecyclerview() {

        val smsRecyclerView: RecyclerView = findViewById(R.id.messageRecyclerview)
        var referrals =
            database.daoAccess().getAllReferrals().sortedByDescending { it.timeRecieved }
        val adapter = SmsRecyclerViewAdaper(referrals)

        adapter.onCLickList.add(object : AdapterClicker {
            override fun onClick(referralEntitiy: SmsReferralEntitiy) {
                //call new activity
                var msg:String;
                val jsonObject: JSONObject;
                try {
                     msg =JSONObject(referralEntitiy.jsonData).toString(4)
                }catch (e:JSONException){
                    msg = "Error: "+ e.message
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(referralEntitiy.id)
                    .setMessage(msg)
                    .create().show()
            }
        })
        smsRecyclerView.adapter = adapter
        val layout: RecyclerView.LayoutManager = LinearLayoutManager(this)
        smsRecyclerView.layoutManager = layout
        adapter.notifyDataSetChanged()

    }


    private fun setupStopService() {
        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            if (isServiceStarted) {
                val intent: Intent = Intent(this, SmsService::class.java)
                intent.action = SmsService.STOP_SERVICE
                ContextCompat.startForegroundService(this, intent)
                isServiceStarted = false
            }
        }
    }

    private fun setupStartService() {
        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            checkpermissions()
        }
    }


    private fun checkpermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.RECEIVE_MMS
                    ), 99
                )
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.RECEIVE_MMS
                    ), 99
                )
            }
        } else {
            //permission already granted
            if (!isServiceStarted) {
                startService()
            }

        }
    }

    fun startService() {
        val serviceIntent = Intent(
            this,
            SmsService::class.java
        )
        serviceIntent.action = SmsService.START_SERVICE
        this.application.startService(serviceIntent)
        ContextCompat.startForegroundService(this, serviceIntent)
        isServiceStarted = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 99) {
            //do whatever when permissions are granted
            if (!isServiceStarted) {
                startService()

            }
        }
    }

    override fun newMessageReceived() {
        setuprecyclerview()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serviceToActivityBroadCastReciever)
    }

    interface AdapterClicker{
        fun onClick(referralEntitiy: SmsReferralEntitiy)
    }
}
