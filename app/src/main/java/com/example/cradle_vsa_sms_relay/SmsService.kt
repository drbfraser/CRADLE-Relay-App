package com.example.cradle_vsa_sms_relay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.android.volley.AuthFailureError
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.cradle_vsa_sms_relay.activities.MainActivity
import com.example.cradle_vsa_sms_relay.broadcast_receiver.MessageReciever
import com.example.cradle_vsa_sms_relay.dagger.MyApp
import com.example.cradle_vsa_sms_relay.database.ReferralDatabase
import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.example.cradle_vsa_sms_relay.utilities.UploadReferralWorker
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.NumberFormatException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SmsService : Service(), MultiMessageListener, SharedPreferences.OnSharedPreferenceChangeListener {
    val CHANNEL_ID = "ForegroundServiceChannel"
    private val readingServerUrl =
        "https://cmpt373.csil.sfu.ca:8048/api/patient/reading"

    // localhost
//    private val referralsServerUrl = "http://10.0.2.2:5000/api/referral"
    private val referralSummeriesServerUrl =
        "https://cmpt373.csil.sfu.ca:8048/api/mobile/summarized/follow_up"
    @Inject
    lateinit var database: ReferralDatabase
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var smsReciver: MessageReciever? = null

    private val mBinder: IBinder = MyBinder()

    // let activity know status of retrying the referral uploads etc.
    lateinit var retryTimerListener:RetryTimerListener

    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        (application as MyApp).component.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            val action: String? = intent.action
            if (action.equals(STOP_SERVICE)) {
                stopForeground(true)
                MessageReciever.unbindListener()
                unregisterReceiver(smsReciver)
                this.stopService(intent)
                this.stopSelf()

            } else {
                smsReciver = MessageReciever()
                val intentFilter = IntentFilter()
                intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
                registerReceiver(smsReciver, intentFilter)
                MessageReciever.bindListener(this)
                val input = intent.getStringExtra("inputExtra")
                createNotificationChannel()
                val notificationIntent = Intent(
                    this,
                    MainActivity::class.java
                )
                val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("SMS RELAY SERVICE RUNNING").setContentText(input)
                    .setSmallIcon(R.drawable.ic_launcher_background).setContentIntent(pendingIntent)
                    .build()
                startForeground(1, notification)
                startReuploadingReferralTask()
                sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            }
        }
        return START_STICKY


    }


    private fun startReuploadingReferralTask() {
        if (!sharedPreferences.getBoolean(getString(R.string.reuploadSwitchPrefKey),false)){
            Log.d("bugg","no retry needed")
            return;
        }
        val timeInMinutesString = sharedPreferences.getString(getString(R.string.reuploadListPrefKey),"")
        try {
            val time = timeInMinutesString.toString().toLong()

            val uploadWorkRequest: OneTimeWorkRequest =
                OneTimeWorkRequest.Builder(UploadReferralWorker::class.java)
                    .setInitialDelay(time, TimeUnit.MINUTES).build()

            WorkManager.getInstance(this)
                .enqueueUniqueWork("work", ExistingWorkPolicy.KEEP, uploadWorkRequest)
            val intent = Intent()
            intent.action = "retryTimerUpdate"
            sendBroadcast(intent)
            Log.d("bugg","task started " + timeInMinutesString)
        } catch (e:NumberFormatException){
            Log.d("bugg","retry time not set "+ timeInMinutesString)
        }
    }


    private fun sendToServer(smsReferralEntitiy: SmsReferralEntitiy) {
        val sharedPref =
            getSharedPreferences(AUTH_PREF, Context.MODE_PRIVATE)
        val token = sharedPref.getString(TOKEN, "")

        var json: JSONObject? = null
        try {
            json = JSONObject(smsReferralEntitiy.jsonData)
        } catch (e: JSONException) {
            smsReferralEntitiy.errorMessage = "Not a valid JSON format"
            updateDatabase(smsReferralEntitiy, false)
            e.printStackTrace()
            //no need to send it to the server, we know its not a valid json
            return
        }
        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            POST, referralsServerUrl, json, Response.Listener { response: JSONObject? ->
                updateDatabase(smsReferralEntitiy, true)
            },
            Response.ErrorListener { error: VolleyError ->
                var json: String? = ""
                try {
                    if (error.networkResponse != null) {
                        json = String(
                            error.networkResponse.data,
                            Charset.forName(HttpHeaderParser.parseCharset(error.networkResponse.headers))
                        )
                        smsReferralEntitiy.errorMessage = json.toString()
                    }
                } catch (e: UnsupportedEncodingException) {
                    smsReferralEntitiy.errorMessage =
                        "No clue whats going on, return message is null"
                    e.printStackTrace()
                }
                //giving back extra info based on status code
                if (error.networkResponse != null) {
                    if (error.networkResponse.statusCode >= 500) {
                        smsReferralEntitiy.errorMessage += " Please make sure referral has all the fields"
                    } else if (error.networkResponse.statusCode >= 400) {
                        smsReferralEntitiy.errorMessage += " Invalid request, make sure you have correct credentials"
                    }
                } else {
                    smsReferralEntitiy.errorMessage = "Unable to get error message"
                }
                updateDatabase(smsReferralEntitiy, false)

            }
        ) {
            /**
             * Passing some request headers
             */
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val header: MutableMap<String, String> =
                    HashMap()
                header[AUTH] = "Bearer $token"
                return header
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.add(jsonObjectRequest)
    }

    fun updateDatabase(smsReferralEntitiy: SmsReferralEntitiy, isUploaded: Boolean) {
        smsReferralEntitiy.isUploaded = isUploaded
        smsReferralEntitiy.numberOfTriesUploaded += 1
        AsyncTask.execute {
            database.daoAccess().updateSmsReferral(smsReferralEntitiy)
            val intent = Intent()
            intent.action = "messageUpdate"
            sendBroadcast(intent)
        }
    }

    override fun stopService(name: Intent?): Boolean {
        super.stopService(name)
        stopForeground(true)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        //cancel all the calls
        WorkManager.getInstance(this).cancelAllWork();
        stopSelf()
        onDestroy()
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        val STOP_SERVICE = "STOP SERVICE"
        val START_SERVICE = "START SERVICE"
        val AUTH_PREF = "authSharefPref"
        val TOKEN = "token"
        val AUTH = "Authorization"
        val USER_ID = "userId"
        val referralsServerUrl = "https://cmpt373.csil.sfu.ca:8048/api/referral"
    }

    override fun messageMapRecieved(smsReferralList: ArrayList<SmsReferralEntitiy>) {

        smsReferralList.forEach { f -> database.daoAccess().insertSmsReferral(f) }
        smsReferralList.forEach { f ->
            sendToServer(f)
        }
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
            val switchkey = getString(R.string.reuploadSwitchPrefKey)
        if (p1.equals(switchkey)){
            retryTimerListener.onRetryTimeChanged(99999999)
        }
    }
    inner class MyBinder : Binder() {
        val service: SmsService
            get() =// clients can call public methods
                this@SmsService
    }
}