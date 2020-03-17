package com.example.cradle_vsa_sms_relay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.AuthFailureError
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.cradle_vsa_sms_relay.activities.MainActivity
import com.example.cradle_vsa_sms_relay.broad_castrecivers.MessageReciever
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class SmsService : Service(), MessageListener {
    val CHANNEL_ID = "ForegroundServiceChannel"
    private val readingServerUrl =
        "https://cmpt373.csil.sfu.ca:8048/api/patient/reading"
    private val referralsServerUrl = "https://cmpt373.csil.sfu.ca:8048/api/referral"
    private val referralSummeriesServerUrl =
        "https://cmpt373.csil.sfu.ca:8048/api/mobile/summarized/follow_up"


    private var smsReciver: MessageReciever? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            val action: String? = intent.action
            if (action.equals(STOP_SERVICE)) {
                Log.d("bugg", "stop service..")
                stopForeground(true)
                MessageReciever.unbindListener()
                unregisterReceiver(smsReciver)
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
            }
        }
        return START_NOT_STICKY


    }

    override fun messageRecieved(message: Sms) {
        sendToServer(message.messageBody)
    }

    private fun sendToServer(body: String) {
        val sharedPref =
            getSharedPreferences(AUTH_PREF, Context.MODE_PRIVATE)
        val token = sharedPref.getString(TOKEN, "")

        var json: JSONObject? = null
        try {
            json = JSONObject(body)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            POST, referralsServerUrl, json, Response.Listener { response: JSONObject? ->

                // letting the activity know if upload was successful
                val intent = Intent();
                val bundle = Bundle();
                bundle.putString("sms",body)
                bundle.putInt("status",UPLOAD_SUCCESSFUL)
                intent.putExtras(bundle)
                intent.setAction("update")
                //received by the activity through ServiceTOActivityBroadcast
                sendBroadcast(intent)
            },
            Response.ErrorListener { error: VolleyError ->
                val intent = Intent();
                val bundle = Bundle();
                bundle.putString("sms",body)
                bundle.putInt("status", UPLOAD_FAIL)
                intent.putExtras(bundle)
                intent.setAction("update")
                sendBroadcast(intent)
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

    override fun stopService(name: Intent?): Boolean {
        super.stopService(name)
        stopForeground(true)
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
        val UPLOAD_SUCCESSFUL = 1;
        val UPLOAD_FAIL =-1
    }
}