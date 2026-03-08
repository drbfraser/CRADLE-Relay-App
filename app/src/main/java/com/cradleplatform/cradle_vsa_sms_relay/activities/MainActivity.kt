package com.cradleplatform.cradle_vsa_sms_relay.activities

import android.Manifest
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_DENIED
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.adapters.MainRecyclerViewAdapter
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestResult
import com.cradleplatform.cradle_vsa_sms_relay.service.SmsService
import com.cradleplatform.cradle_vsa_sms_relay.view_model.SmsRelayViewModel
import com.google.android.material.button.MaterialButton

@Suppress("LargeClass", "TooManyFunctions")
class MainActivity : AppCompatActivity(), MainRecyclerViewAdapter.OnItemClickListener {

    private var stopServiceDialog: AlertDialog? = null

    // our reference to the service
    var mService: SmsService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            mService = null
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as SmsService.MyBinder
            mService = binder.service
        }
    }

    private lateinit var smsRelayViewModel: SmsRelayViewModel
    private lateinit var mainRecyclerViewAdapter: MainRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MyApp).component.inject(this)
        mainRecyclerViewAdapter = setupRecyclerView()
        mainRecyclerViewAdapter.setOnItemClickListener(this)
        setupToolBar()
        setupStartService()
        setupStopService()
        setupStopServiceDialog()
        setupFilter()
        observeServiceState()
    }

    override fun onStart() {
        super.onStart()
        // Re-bind to service every time activity becomes visible (handles rotation)
        if (smsRelayViewModel.isServiceStarted.value == true) {
            bindToRunningService()
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind on every stop (rotation or navigating away) to avoid leaking connection
        if (mService != null) {
            unbindService(serviceConnection)
            mService = null
        }
    }

    override fun onItemClick(position: Int) {
        val relayRequest = mainRecyclerViewAdapter.sms[position] // Access the item from the list
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra("phoneNumber", relayRequest.phoneNumber)
            putExtra("requestId", relayRequest.requestId.toString())
            // Pass any other relevant data here
        }
        startActivity(intent)
    }

    private fun observeServiceState() {
        smsRelayViewModel.isServiceStarted.observe(this) { started ->
            makeButtonUnclickable(started)
        }
    }

    private fun setupFilter() {
        val phoneNumberSpinner: Spinner = findViewById(R.id.phoneNumberSpinner)
        val filterTypeSpinner: Spinner = findViewById(R.id.filterType)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mainRecyclerViewAdapter.getPhoneNumbers()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        phoneNumberSpinner.adapter = adapter

        val filterAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.filter_options,
            android.R.layout.simple_spinner_item
        )
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterTypeSpinner.adapter = filterAdapter

        // Restore saved spinner positions
        val savedPhone = smsRelayViewModel.selectedPhoneNumber.value
        if (savedPhone != null) {
            val pos = (0 until adapter.count).firstOrNull { adapter.getItem(it) == savedPhone } ?: 0
            phoneNumberSpinner.setSelection(pos)
        }
        val savedFilterIndex = smsRelayViewModel.selectedFilterIndex.value ?: 0
        filterTypeSpinner.setSelection(savedFilterIndex)

        phoneNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val phone = parent?.getItemAtPosition(position).toString()
                smsRelayViewModel.setSelectedPhoneNumber(phone)
                filterList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        filterTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                smsRelayViewModel.setSelectedFilterIndex(position)
                filterList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterList() {
        val selectedPhone = smsRelayViewModel.selectedPhoneNumber.value
        val selectedFilterIndex = smsRelayViewModel.selectedFilterIndex.value ?: 0
        val selectedFilter = resources.getStringArray(R.array.filter_options)
            .getOrElse(selectedFilterIndex) { "None" }

        val filteredList = if (selectedPhone == null || selectedPhone == "All") {
            smsRelayViewModel.getAllRelayRequests().value.orEmpty()
        } else {
            smsRelayViewModel.getAllRelayRequests().value.orEmpty()
                .filter { it.phoneNumber == selectedPhone }
        }

        val finalFilteredList = when (selectedFilter) {
            "Only Successful" -> filteredList.filter { it.requestResult == RelayRequestResult.OK }
            "Only Failed" -> filteredList.filter { it.requestResult == RelayRequestResult.ERROR }
            else -> filteredList
        }

        mainRecyclerViewAdapter.setRelayList(finalFilteredList)
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

    private fun setupRecyclerView(): MainRecyclerViewAdapter {
        val emptyImageView: ScrollView = findViewById(R.id.emptyRecyclerView)
        val smsRecyclerView: RecyclerView = findViewById(R.id.messageRecyclerview)
        val adapter = MainRecyclerViewAdapter()
        smsRecyclerView.adapter = adapter
        val layout: RecyclerView.LayoutManager = LinearLayoutManager(this)
        smsRecyclerView.layoutManager = layout

        smsRelayViewModel =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
                SmsRelayViewModel::class.java
            )

        smsRelayViewModel.getAllRelayRequests().observe(
            this
        ) { relayEntities ->
            // update the recyclerview on updating
            if (relayEntities.isNotEmpty()) {
                emptyImageView.visibility = GONE
            } else {
                emptyImageView.visibility = VISIBLE
            }

            adapter.setRelayList(relayEntities.sortedByDescending { it.timeMsInitiated })
            adapter.notifyDataSetChanged()
        }

        return adapter
    }

    private fun setupStopService() {
        findViewById<MaterialButton>(R.id.btnStopService).setOnClickListener {
            if (smsRelayViewModel.isServiceStarted.value != true) {
                Toast.makeText(this, "Service is not running", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            smsRelayViewModel.requestStopServiceDialog()
        }
    }

    private fun setupStopServiceDialog() {
        smsRelayViewModel.showStopServiceDialog.observe(this) { shouldShow ->
            if (shouldShow) {
                // Dismiss any stale dialog instance before creating a new one
                stopServiceDialog?.takeIf { it.isShowing }?.dismiss()
                stopServiceDialog = null

                val alertDialog = AlertDialog.Builder(this).create()
                val view = layoutInflater.inflate(R.layout.stop_service_dialog, null)
                alertDialog.setView(view)
                alertDialog.setOnDismissListener {
                    // Only reset ViewModel state if this activity is not being rotated.
                    // On rotation the new Activity's observer will re-show the dialog.
                    if (!isChangingConfigurations) {
                        smsRelayViewModel.dismissStopServiceDialog()
                    }
                    stopServiceDialog = null
                }
                view.findViewById<Button>(R.id.yesButton).setOnClickListener {
                    alertDialog.dismiss()
                    stopSmsService()
                }
                view.findViewById<Button>(R.id.noButton).setOnClickListener {
                    alertDialog.dismiss()
                }
                alertDialog.show()
                stopServiceDialog = alertDialog
            } else {
                stopServiceDialog?.takeIf { it.isShowing }?.dismiss()
                stopServiceDialog = null
            }
        }
    }

    private fun stopSmsService() {
        if (smsRelayViewModel.isServiceStarted.value == true) {
            // Unbind first if still connected
            if (mService != null) {
                unbindService(serviceConnection)
                mService = null
            }
            val intent = Intent(this, SmsService::class.java)
            intent.action = SmsService.STOP_SERVICE
            ContextCompat.startForegroundService(this, intent)
            smsRelayViewModel.setServiceStarted(false)
        }
    }

    private fun makeButtonUnclickable(serviceStarted: Boolean) {
        val statusTxt = findViewById<TextView>(R.id.serviceStatusTxt)
        val startButton = findViewById<MaterialButton>(R.id.btnStartService)
        val stopButton = findViewById<MaterialButton>(R.id.btnStopService)

        if (!serviceStarted) {
            statusTxt.text = getString(R.string.stop_service_status)
            statusTxt.setTextColor(ContextCompat.getColor(this, R.color.redDown))
            stopButton.alpha = ALPHA_LOW
            stopButton.isClickable = false
            startButton.alpha = ALPHA_HIGH
            startButton.isClickable = true
        } else {
            statusTxt.text = getString(R.string.start_service_status)
            statusTxt.setTextColor(ContextCompat.getColor(this, R.color.green))
            startButton.alpha = ALPHA_LOW
            startButton.isClickable = false
            stopButton.alpha = ALPHA_HIGH
            stopButton.isClickable = true
        }
    }

    private fun setupStartService() {
        findViewById<MaterialButton>(R.id.btnStartService).setOnClickListener {
            checkPermissions()
        }
        // Always sync ViewModel to the real service running state — this is the single
        // source of truth. Never auto-start here; that would restart the service on rotation.
        val serviceActuallyRunning = SmsService.isServiceRunningInForeground(this, SmsService::class.java)
        smsRelayViewModel.setServiceStarted(serviceActuallyRunning)
    }

    private fun bindToRunningService() {
        val intent = Intent(this, SmsService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.SEND_SMS
                    ),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // permission already granted
            if (smsRelayViewModel.isServiceStarted.value != true) {
                startService()
            }
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, SmsService::class.java)
        serviceIntent.action = SmsService.START_SERVICE
        ContextCompat.startForegroundService(this, serviceIntent)
        // Do not call bindService here — onStart handles binding via bindToRunningService()
        smsRelayViewModel.setServiceStarted(true)
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
                if (it == PERMISSION_DENIED) {
                    return
                }
            }
            // do whatever when permissions are granted
            if (smsRelayViewModel.isServiceStarted.value != true) {
                startService()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // onStop already unbinds. Nothing extra needed here.
    }

    companion object {
        const val ALPHA_LOW = 0.2F
        const val ALPHA_HIGH = 1.0F
        const val PERMISSION_REQUEST_CODE = 99
    }
}
