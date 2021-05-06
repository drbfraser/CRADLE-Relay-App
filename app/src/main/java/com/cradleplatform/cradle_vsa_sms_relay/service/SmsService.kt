package com.cradleplatform.smsrelay.service

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
import com.cradleplatform.smsrelay.R
import com.cradleplatform.smsrelay.activities.MainActivity
import com.cradleplatform.smsrelay.broadcast_receiver.MessageReciever
import com.cradleplatform.smsrelay.dagger.MyApp
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.smsrelay.database.SmsReferralEntity
import com.cradleplatform.smsrelay.network.Failure
import com.cradleplatform.smsrelay.network.NetworkManager
import com.cradleplatform.smsrelay.network.Success
import com.cradleplatform.smsrelay.network.VolleyRequests
import com.cradleplatform.smsrelay.utilities.UploadReferralWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

@Suppress("LargeClass", "TooManyFunctions")
class SmsService : LifecycleService(),
    SharedPreferences.OnSharedPreferenceChangeListener, CoroutineScope {

    private val coroutineScope by lazy { CoroutineScope(coroutineContext) }

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext by lazy { Dispatchers.IO + coroutineJob }

    @Inject
    lateinit var referralRepository: ReferralRepository

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var networkManager: NetworkManager

    // maain sms broadcast listner
    private var smsReciver: MessageReciever? = null

    // to make sure we dont keep registering listerners
    private var isMessageRecieverRegistered = false

    // handles activity to service interactions
    private val mBinder: IBinder = MyBinder()

    private val referralObserver = Observer<List<SmsReferralEntity>> { referralList ->
        referralList.forEach {
            sendToServer(it)
        }
    }

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
        if (intent == null) {
            return START_STICKY
        }
        val action: String? = intent.action
        if (action.equals(STOP_SERVICE)) {
            stopForeground(true)
            smsReciver?.updateLastRunPref()
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
                isMessageRecieverRegistered = true
                referralRepository.getAllUnUploadedLiveListReferral()
                    .observe(this, referralObserver)
                // ask the receiver to fetch all the unsent messages since sms service was last
                // started
                smsReciver?.getUnsentSms()
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
                            // this is where we notify user but right now dont have a good mechanism
                            // periodice work state is enqued->running->enque
                            // since there is no success or failure state we cant let user know
                            // extactly whats going on.
                            if (it.state != WorkInfo.State.ENQUEUED) {
                                // todo figure out what to do :(
                                //  notificationForReuploading(it, false)
                            } else {
                                // notificationForReuploading(it, true)
                            }
                        }
                    })
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

    private fun notificationForReuploading(it: WorkInfo?, cancel: Boolean) {

        val notificationManager =
            NotificationManagerCompat.from(this)
        if (cancel) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Retrying uploading referrals ")
            .setContentText("" + cancel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * uploads [smsReferralEntity] to the server
     * updates the status of the upload to the database.
     */
    fun sendToServer(smsReferralEntity: SmsReferralEntity) {
        try {
            JSONObject(smsReferralEntity.jsonData.toString())
        } catch (e: JSONException) {
            smsReferralEntity.errorMessage = "Not a valid JSON format"
            updateDatabase(smsReferralEntity, false)
            e.printStackTrace()
            // no need to send it to the server, we know its not a valid json
            return
        }

        networkManager.uploadReferral(smsReferralEntity) {
            when (it) {
                is Success -> {
                    updateDatabase(smsReferralEntity, true)
                }

                is Failure -> {
                    smsReferralEntity.errorMessage = VolleyRequests.getServerErrorMessage(it.value)
                    updateDatabase(smsReferralEntity, false)
                }
            }
        }
    }

    /**
     * updates the room database and notifies [singleMessageListener] of the new message
     */
    private fun updateDatabase(smsReferralEntity: SmsReferralEntity, isUploaded: Boolean) {

        coroutineScope.launch {
            // Use SmsManager to send delivery confirmation
            // todo get delivery confirmation for us as well
            smsReferralEntity.isUploaded = isUploaded
            val smsManager = SmsManager.getDefault()
            smsManager.sendMultipartTextMessage(
                smsReferralEntity.phoneNumber, null,
                smsManager.divideMessage(constructDeliveryMessage(smsReferralEntity)),
                null, null
            )
            smsReferralEntity.deliveryReportSent = true
            if (isUploaded) {
                // we do not need to show anymore errors for this referral.
                smsReferralEntity.errorMessage = ""
            }
            smsReferralEntity.numberOfTriesUploaded += 1
            referralRepository.update(smsReferralEntity)
        }
    }

    private fun constructDeliveryMessage(smsReferralEntity: SmsReferralEntity): String {
        val stringBuilder = StringBuilder()
        if (smsReferralEntity.isUploaded) {
            stringBuilder.append("Referral delivered")
        } else {
            stringBuilder.append("Referral NOT delivered")
        }
        stringBuilder.append("\nID: ").append(smsReferralEntity.id)

        if (!smsReferralEntity.isUploaded) {
            stringBuilder.append("\nERROR: ").append(smsReferralEntity.errorMessage)
        }
        return stringBuilder.toString()
    }

    override fun stopService(name: Intent?): Boolean {
        super.stopService(name)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        referralRepository.referrals.removeObserver(referralObserver)
        // cancel all the calls
        WorkManager.getInstance(this).cancelAllWork()
        stopSelf()
        onDestroy()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob.cancel()
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
        const val CHANNEL_ID = "ForegroundServiceChannel"
        const val NOTIFICATION_ID = 99
        const val STOP_SERVICE = "STOP SERVICE"
        const val START_SERVICE = "START SERVICE"

        // todo change this
        const val referralsServerUrl = "http://10.0.2.2:5000/api/referral"

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

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        val switchkey = getString(R.string.reuploadSwitchPrefKey)
        val listKey = getString(R.string.reuploadListPrefKey)
        val syncNowKey = getString(R.string.sync_now_key)
        // restart sending service if time to send changes or the decision to send changes.
        if (key.equals(listKey) ||
            (key.equals(switchkey) && sharedPreferences.getBoolean(switchkey, false))
        ) {
            startReuploadingReferralTask()
        } else if (key.equals(syncNowKey)) {
            Toast.makeText(this, getString(R.string.service_running_sync_toast), Toast.LENGTH_LONG)
                .show()
            startReuploadingReferralTask()
        }
    }

    inner class MyBinder : Binder() {
        val service: SmsService
            get() = // clients can call public methods
                this@SmsService
    }
}
