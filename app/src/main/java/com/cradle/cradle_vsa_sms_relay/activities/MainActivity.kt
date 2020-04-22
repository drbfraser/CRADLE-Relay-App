package com.cradle.cradle_vsa_sms_relay.activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.cradle.cradle_vsa_sms_relay.*
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.ReferralDatabase
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.views.ReferralAlertDialog
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
    SingleMessageListener {

    private var isServiceStarted = false
    var mIsBound: Boolean = false
    //our reference to the service
    var mService: SmsService? = null
    @Inject
    lateinit var database: ReferralDatabase
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            mIsBound = false
            mService = null
            isServiceStarted = false
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as SmsService.MyBinder
            mIsBound=true
            isServiceStarted= true
            mService = binder.service
            mService?.singleMessageListener = this@MainActivity

            mService?.reuploadReferralListener = object : ReuploadReferralListener {
                override fun onReuploadReferral(workInfo: WorkInfo) {
                    if (workInfo.state == WorkInfo.State.RUNNING) {
                        Toast.makeText(this@MainActivity, "Uploading failed referrals", Toast.LENGTH_SHORT)
                            .show()
                        //update recylcer view
                        setuprecyclerview()
                    }
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MyApp).component.inject(this)
        // bind service in case its running
        if (mService == null) {
            val serviceIntent = Intent(
                this,
                SmsService::class.java
            )
            bindService(serviceIntent, serviceConnection, 0)
        }
        setupStartService()
        setupStopService()
        setuprecyclerview()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settingMenu -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun setuprecyclerview() {

        val smsRecyclerView: RecyclerView = findViewById(R.id.messageRecyclerview)
        var referrals =
            database.daoAccess().getAllReferrals().sortedByDescending { it.timeRecieved }
        val adapter = SmsRecyclerViewAdaper(referrals)

        adapter.onCLickList.add(object : AdapterClicker {
            override fun onClick(referralEntitiy: SmsReferralEntitiy) {
                val referralAlertDialog =ReferralAlertDialog(this@MainActivity,referralEntitiy)

                referralAlertDialog.setOnSendToServerListener(View.OnClickListener {
                    if (isServiceStarted && mIsBound){
                        if (!referralEntitiy.isUploaded) {
                            mService?.sendToServer(referralEntitiy)
                            Toast.makeText(
                                this@MainActivity, "Uploading the referral to the server",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else{
                            Toast.makeText(
                                this@MainActivity, "Referral is already uploaded to the server ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        referralAlertDialog.cancel()
                    } else {
                        Toast.makeText(this@MainActivity,"Unable to send to the server, " +
                                "Make sure service is running.",Toast.LENGTH_SHORT).show()
                    }
                })
                referralAlertDialog.show()
            }
        })
        smsRecyclerView.adapter = adapter
        val layout: RecyclerView.LayoutManager = LinearLayoutManager(this)
        smsRecyclerView.layoutManager = layout
        adapter.notifyDataSetChanged()

    }


    private fun setupStopService() {
        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            if (mService != null && isServiceStarted) {
                val intent: Intent = Intent(this, SmsService::class.java).also { intent ->
                    unbindService(serviceConnection)
                }
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
        ).also { intent -> bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE) }
        serviceIntent.action = SmsService.START_SERVICE
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
        runOnUiThread {
            setuprecyclerview()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mIsBound) {
            unbindService(serviceConnection)
        }
    }

    interface AdapterClicker {
        fun onClick(referralEntitiy: SmsReferralEntitiy)
    }
}
