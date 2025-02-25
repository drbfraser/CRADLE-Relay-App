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
import android.widget.FrameLayout
import android.widget.ImageButton
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

    private var isServiceStarted = false
    private var selectedPhoneNumber: String? = null

    // our reference to the service
    var mService: SmsService? = null

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
        setupFilter()
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

    private fun setupFilter() {
        // Get reference to the spinner
        val phoneNumberSpinner: Spinner = findViewById(R.id.phoneNumberSpinner)
        val filterTypeSpinner: Spinner = findViewById(R.id.filterType)

        // Create an ArrayAdapter using the MainRecyclerViewAdapter's phone numbers
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mainRecyclerViewAdapter.getPhoneNumbers()
        )

        // Set the layout for the dropdown list
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set the adapter to the spinner
        phoneNumberSpinner.adapter = adapter

        // Set up filterTypeSpinner with options: None, Only Successful, and Only Failed
        val filterAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.filter_options,
            android.R.layout.simple_spinner_item
        )
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterTypeSpinner.adapter = filterAdapter

        // Add a listener to the phoneNumberSpinner to filter the RecyclerView based on the selected phone number
        phoneNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Get the selected phone number
                selectedPhoneNumber = parent?.getItemAtPosition(position).toString()

                // Filter the list of SMS relay entities based on the selected phone number and filter type
                filterList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing when nothing is selected
            }
        }

        // Add a listener to the filterTypeSpinner to filter the RecyclerView based on the selected filter type
        filterTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Filter the list of SMS relay entities based on the selected filter type
                filterList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing when nothing is selected
            }
        }
    }

    private fun filterList() {
        // Get the selected filter type
        val selectedFilter = findViewById<Spinner>(R.id.filterType).selectedItem.toString()

        // Filter the list of SMS relay entities based on the selected phone number and filter type
        val filteredList = if (selectedPhoneNumber == "All") {
            smsRelayViewModel.getAllRelayRequests().value.orEmpty()
        } else {
            smsRelayViewModel.getAllRelayRequests().value.orEmpty()
                .filter { it.phoneNumber == selectedPhoneNumber }
        }

        // Apply additional filtering based on the selected filter type
        val finalFilteredList = when (selectedFilter) {
            "Only Successful" -> filteredList.filter { it.requestResult == RelayRequestResult.OK }
            "Only Failed" -> filteredList.filter { it.requestResult == RelayRequestResult.ERROR }
            else -> filteredList
        }

        // Update the RecyclerView to display only the filtered SMS relay entities
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
        val emptyImageView: FrameLayout = findViewById(R.id.emptyRecyclerView)
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
            val intent: Intent = Intent(this, SmsService::class.java).also { _ ->
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
        // start the service initially
        checkPermissions()
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
                if (it == PERMISSION_DENIED) {
                    return
                }
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

    companion object {
        const val ALPHA_LOW = 0.2F
        const val ALPHA_HIGH = 1.0F
        const val PERMISSION_REQUEST_CODE = 99
    }
}
