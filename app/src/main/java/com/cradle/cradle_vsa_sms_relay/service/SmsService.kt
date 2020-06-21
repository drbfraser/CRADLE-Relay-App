package com.cradle.cradle_vsa_sms_relay.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.android.volley.AuthFailureError
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.cradle.cradle_vsa_sms_relay.MultiMessageListener
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.activities.MainActivity
import com.cradle.cradle_vsa_sms_relay.broadcast_receiver.MessageReciever
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.ReferralRepository
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.utilities.DateTimeUtil
import com.cradle.cradle_vsa_sms_relay.utilities.UploadReferralWorker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.HashMap

class SmsService : LifecycleService(),
    MultiMessageListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    val CHANNEL_ID = "ForegroundServiceChannel"

    @Inject
    lateinit var referralRepository: ReferralRepository

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    // maain sms broadcast listner
    private var smsReciver: MessageReciever? = null

    //to make sure we dont keep registering listerners
    private var isMessageRecieverRegistered = false

    //handles activity to service interactions
    private val mBinder: IBinder = MyBinder()

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
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
                smsReciver?.unbindListener()
                if (smsReciver != null) {
                    unregisterReceiver(smsReciver)
                }
                smsReciver = null
                this.stopService(intent)
                this.stopSelf()

            } else {
                if (!isMessageRecieverRegistered) {
                    smsReciver = MessageReciever(this)
                    val intentFilter = IntentFilter()
                    intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
                    registerReceiver(smsReciver, intentFilter)
                    smsReciver?.bindListener(this)
                    isMessageRecieverRegistered = true
                }
                val input = intent.getStringExtra("inputExtra")
                createNotificationChannel()
                val notificationIntent = Intent(
                    this,
                    MainActivity::class.java
                )
                val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("SMS RELAY SERVICE RUNNING").setContentText(input)
                    .setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pendingIntent)
                    .build()
                startForeground(1, notification)
                startReuploadingReferralTask()
                sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            }
        }
        return START_STICKY


    }

    /**
     * This function starts the periodic tasks to reupload all the referrals that failed to upload before.
     * Uses the time selected by user in settings preference.
     */
    private fun startReuploadingReferralTask() {
        // cancel previous calls
        WorkManager.getInstance(this).cancelAllWork()

        val timeInMinutesString =
            sharedPreferences.getString(getString(R.string.reuploadListPrefKey), "")
        try {
            val time = timeInMinutesString.toString().toLong()

            val uploadWorkRequest: PeriodicWorkRequest =
                PeriodicWorkRequest.Builder(
                    UploadReferralWorker::class.java,
                    time,
                    TimeUnit.MINUTES
                ).addTag("reuploadTag").build()
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    "work",
                    ExistingPeriodicWorkPolicy.KEEP,
                    uploadWorkRequest
                )
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(uploadWorkRequest.id)
                .observeForever(
                    Observer {
                        if (it != null) {
                            //this is where we notify user but right now dont have a good mechanism
                            //periodice work state is enqued->running->enque
                            //since there is no success or failure state we cant let user know
                            //extactly whats going on.
                            if (it.state != WorkInfo.State.ENQUEUED) {
                                //todo figure out what to do :(
                                //  notificationForReuploading(it, false)
                            } else {
                                // notificationForReuploading(it, true)
                            }
                        }
                    })
        } catch (e: NumberFormatException) {
        }
    }

    private fun notificationForReuploading(it: WorkInfo?, cancel: Boolean) {

        val notificationManager =
            NotificationManagerCompat.from(this)
        if (cancel) {
            notificationManager.cancel(99)
            return
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Retrying uploading referrals ")
            .setContentText("" + cancel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(99, builder.build())

    }


    /**
     * uploads [smsReferralEntitiy] to the server
     * updates the status of the upload to the database.
     */
    fun sendToServer(smsReferralEntitiy: SmsReferralEntitiy) {

        val token = sharedPreferences.getString(TOKEN, "")

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
            POST,
            referralsServerUrl, json, Response.Listener { response: JSONObject? ->
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
                    } else {
                        smsReferralEntitiy.errorMessage+= error.localizedMessage.toString()
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

    /**
     * updates the room database and notifies [singleMessageListener] of the new message
     */
    fun updateDatabase(smsReferralEntitiy: SmsReferralEntitiy, isUploaded: Boolean) {

        MainScope().launch {
            // Use SmsManager to send delivery confirmation
            //todo get delivery confirmation for us as well
            val smsManager = SmsManager.getDefault()
            smsManager.sendMultipartTextMessage(
                smsReferralEntitiy.phoneNumber, null,
                smsManager.divideMessage(constructDeliveryMessage(smsReferralEntitiy)),
                null, null
            )
            smsReferralEntitiy.isUploaded = isUploaded
            smsReferralEntitiy.deliveryReportSent = true
            if (isUploaded) {
                // we do not need to show anymore errors for this referral.
                smsReferralEntitiy.errorMessage = ""
            }
            smsReferralEntitiy.numberOfTriesUploaded += 1
            referralRepository.update(smsReferralEntitiy)
        }
    }

    private fun constructDeliveryMessage(smsReferralEntitiy: SmsReferralEntitiy): String {
        val stringBuilder = StringBuilder()
        val timeString = DateTimeUtil.convertUnixToTimeString(smsReferralEntitiy.timeRecieved)
        stringBuilder.append("referral Id: ").append(smsReferralEntitiy.id)
            .append("\nTime received: ").append(timeString)
            .append("\nSuccessfully sent to the health care facility? : ")

        if (smsReferralEntitiy.isUploaded) {
            stringBuilder.append("YES\n")
        } else {
            stringBuilder.append("NO\n")
                .append("ERROR: ").append(smsReferralEntitiy.errorMessage)
                .append("\n")
        }
        return stringBuilder.toString()
    }

    override fun stopService(name: Intent?): Boolean {
        super.stopService(name)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        //cancel all the calls
        WorkManager.getInstance(this).cancelAllWork()
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
        val TOKEN = "token"
        val AUTH = "Authorization"
        val USER_ID = "userId"
        val referralsServerUrl = "https://cmpt373-lockdown.cs.surrey.sfu.ca/api/referral"

        /**
         * https://stackoverflow.com/questions/6452466/how-to-determine-if-an-android-service-is-running-in-the-foreground
         */
        fun isServiceRunningInForeground(
            context: Context,
            serviceClass: Class<*>
        ): Boolean {
            val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className && service.foreground) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * inserts the [smsReferralList] into the Database and sends the list to the server
     */
    override fun messageMapRecieved(smsReferralList: List<SmsReferralEntitiy>) {
        referralRepository.insertAll(smsReferralList)
        smsReferralList.forEach { f ->
            sendToServer(f)
        }
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val switchkey = getString(R.string.reuploadSwitchPrefKey)
        val listKey = getString(R.string.reuploadListPrefKey)
        val syncNowKey = getString(R.string.sync_now_key)
        // restart sending service if time to send changes or the decision to send changes.
        if (key.equals(listKey) ||
            (key.equals(switchkey) && sharedPreferences.getBoolean(switchkey, false))) {
            startReuploadingReferralTask()
        } else if (key.equals(syncNowKey)){
            Toast.makeText(this,getString(R.string.service_running_sync_toast), Toast.LENGTH_LONG).show()
            startReuploadingReferralTask()
        }
    }

    inner class MyBinder : Binder() {
        val service: SmsService
            get() =// clients can call public methods
                this@SmsService
    }
}