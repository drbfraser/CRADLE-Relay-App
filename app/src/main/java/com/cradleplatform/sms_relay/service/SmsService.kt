package com.cradleplatform.sms_relay.service

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
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.work.WorkManager
import com.cradleplatform.sms_relay.R
import com.cradleplatform.sms_relay.activities.MainActivity
import com.cradleplatform.sms_relay.broadcast_receiver.MessageReceiver
import com.cradleplatform.sms_relay.dagger.MyApp
import com.cradleplatform.sms_relay.network.NetworkManager
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@Suppress("LargeClass", "TooManyFunctions")
class SmsService : LifecycleService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext by lazy { Dispatchers.IO + coroutineJob }

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var networkManager: NetworkManager

    // main sms broadcast listener
    private var smsReceiver: MessageReceiver? = null

    // to make sure we don't keep registering listeners
    private var isMessageReceiverRegistered = false

    // handles activity to service interactions
    private val mBinder: IBinder = MyBinder()

    override fun onBind(intent: Intent): IBinder {
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
            smsReceiver?.updateLastRunPref()
            if (smsReceiver != null) {
                unregisterReceiver(smsReceiver)
            }
            smsReceiver = null
            this.stopService(intent)
            this.stopSelf()
        } else {
            if (!isMessageReceiverRegistered) {
                smsReceiver = MessageReceiver(this)
                val intentFilter = IntentFilter()
                intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
                intentFilter.priority = Int.MAX_VALUE
                registerReceiver(smsReceiver, intentFilter)
                isMessageReceiverRegistered = true
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
        }
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        super.stopService(name)
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

        /**
         * https://stackoverflow.com/questions/6452466/how-to-determine-if-an-android-service-is-running-in-the-foreground
         */
        fun isServiceRunningInForeground(
            context: Context,
            serviceClass: Class<*>
        ): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className && service.foreground) {
                    return true
                }
            }
            return false
        }
    }

    inner class MyBinder : Binder() {
        val service: SmsService
            get() = // clients can call public methods
                this@SmsService
    }
}
