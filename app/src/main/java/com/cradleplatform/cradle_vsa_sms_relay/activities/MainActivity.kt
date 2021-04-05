package com.cradleplatform.cradle_vsa_sms_relay.activities

import android.Manifest
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_DENIED
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.SmsRecyclerViewAdaper
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradleplatform.cradle_vsa_sms_relay.service.SmsService
import com.cradleplatform.cradle_vsa_sms_relay.view_model.ReferralViewModel
import com.cradleplatform.cradle_vsa_sms_relay.views.ReferralAlertDialog
import com.google.android.material.button.MaterialButton
import javax.inject.Inject

@Suppress("LargeClass", "TooManyFunctions")
class MainActivity : AppCompatActivity() {

    private var isServiceStarted = false

    // our reference to the service
    var mService: SmsService? = null

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            mService = null
            isServiceStarted = false
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as SmsService.MyBinder
            isServiceStarted = true
            mService = binder.service
        }
    }
    private lateinit var referralViewModel: ReferralViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MyApp).component.inject(this)

        setupToolBar()
        setupStartService()
        setupStopService()
        setuprecyclerview()
    }

    private fun setupToolBar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        val settingButton: ImageButton = findViewById(R.id.settingIcon)
        settingButton.setOnClickListener {
            startActivity(
                Intent(this, SettingsActivity::class.java),
                ActivityOptions.makeCustomAnimation(this, R.anim.slide_down, R.anim.nothing)
                    .toBundle()
            )
        }
    }

    private fun setuprecyclerview() {

        val emptyImageView: ImageView = findViewById(R.id.emptyRecyclerView)
        val smsRecyclerView: RecyclerView = findViewById(R.id.messageRecyclerview)
        val adapter = SmsRecyclerViewAdaper(this)
        smsRecyclerView.adapter = adapter
        val layout: RecyclerView.LayoutManager = LinearLayoutManager(this)
        smsRecyclerView.layoutManager = layout
        adapter.onCLickList.add(object : AdapterClicker {
            override fun onClick(referralEntity: SmsReferralEntity) {
                val referralAlertDialog = ReferralAlertDialog(this@MainActivity, referralEntity)

                referralAlertDialog.setOnSendToServerListener(View.OnClickListener {
                    if (isServiceStarted) {
                        if (!referralEntity.isUploaded) {
                            mService?.sendToServer(referralEntity)
                            Toast.makeText(
                                this@MainActivity, "Uploading the referral to the server",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity, "Referral is already uploaded to the server ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        referralAlertDialog.cancel()
                    } else {
                        Toast.makeText(
                            this@MainActivity, "Unable to send to the server, " +
                                    "Make sure service is running.", Toast.LENGTH_SHORT
                        ).show()
                    }
                })
                referralAlertDialog.show()
            }
        })
        referralViewModel =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
                ReferralViewModel::class.java
            )
        referralViewModel.getAllReferrals().observe(this, Observer { referrals ->
            // update the recyclerview on updating
            if (referrals.isNotEmpty()) {
                emptyImageView.visibility = GONE
            } else {
                emptyImageView.visibility = VISIBLE
            }
            adapter.setReferralList(referrals.sortedByDescending { it.timeReceived })
        })
    }

    private fun setupStopService() {
        findViewById<MaterialButton>(R.id.btnStopService).setOnClickListener {
            if (!isServiceStarted) {
                Toast.makeText(this, "Service is not running", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val alertDialog = AlertDialog.Builder(this).create()
            val view = layoutInflater.inflate(R.layout.stop_service_dialog, null)
            alertDialog.setView(view)
            view.findViewById<Button>(R.id.yesButton).setOnClickListener {
                alertDialog.dismiss()
                stopSmsService()
            }
            view.findViewById<Button>(R.id.noButton).setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.show()
        }
    }

    private fun stopSmsService() {
        if (mService != null && isServiceStarted) {
            val intent: Intent = Intent(this, SmsService::class.java).also { intent ->
                unbindService(serviceConnection)
            }
            intent.action = SmsService.STOP_SERVICE
            ContextCompat.startForegroundService(this, intent)
            isServiceStarted = false
            makeButtonUnclickable(false)
        }
    }

    private fun makeButtonUnclickable(serviceStarted: Boolean) {
        val statusTxt = findViewById<TextView>(R.id.serviceStatusTxt)
        val startButton = findViewById<MaterialButton>(R.id.btnStartService)
        val stopButton = findViewById<MaterialButton>(R.id.btnStopService)

        if (!serviceStarted) {
            statusTxt.text = getString(R.string.stop_service_status)
            statusTxt.setTextColor(resources.getColor(R.color.redDown))
            stopButton.alpha = ALPHA_LOW
            stopButton.isClickable = false
            startButton.alpha = ALPHA_HIGH
            startButton.isClickable = true
        } else {
            statusTxt.text = getString(R.string.start_service_status)
            statusTxt.setTextColor(resources.getColor(R.color.green))
            startButton.alpha = ALPHA_LOW
            startButton.isClickable = false
            stopButton.alpha = ALPHA_HIGH
            stopButton.isClickable = true
        }
    }

    private fun setupStartService() {
        findViewById<MaterialButton>(R.id.btnStartService).setOnClickListener {
            checkpermissions()
        }
        // start the service initially
        checkpermissions()
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
                        Manifest.permission.SEND_SMS
                    ), PERMISSION_REQUEST_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.SEND_SMS
                    ), PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // permission already granted
            if (!isServiceStarted) {
                startService()
            }
        }
    }

    private fun startService() {
        val serviceIntent = Intent(
            this,
            SmsService::class.java
        ).also { intent -> bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE) }
        serviceIntent.action = SmsService.START_SERVICE
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, serviceConnection, 0)
        makeButtonUnclickable(true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // need all the permissions
            grantResults.forEach {
                if (it == PERMISSION_DENIED)
                    return
            }
            // do whatever when permissions are granted
            if (!isServiceStarted) {
                startService()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (SmsService.isServiceRunningInForeground(
                this,
                SmsService::class.java
            )
        ) {
            unbindService(serviceConnection)
        }
    }

    interface AdapterClicker {
        fun onClick(referralEntity: SmsReferralEntity)
    }

    companion object {
        const val ALPHA_LOW = 0.2F
        const val ALPHA_HIGH = 1.0F
        const val PERMISSION_REQUEST_CODE = 99
    }
}
