package com.cradle.cradle_vsa_sms_relay.utilities

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.cradle.cradle_vsa_sms_relay.SmsService
import com.cradle.cradle_vsa_sms_relay.SmsService.Companion.TOKEN
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.ReferralDatabase
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import javax.inject.Inject

/**
 * we are using work manager to schedule tasks.
 *
 * "WorkManager API is using JobScheduler, Firebase JobDistpacher or AlarmManager under the hood,
 * you must consider minimum API levels for used functionality. JobScheduler requires
 * minimum API 21, Firebase JobDispatcher requires minimum API 14 and Google Play Services."
 * https://stackoverflow.com/questions/50708993/what-is-the-best-practice-to-use-for-background-tasks
 */
class UploadReferralWorker(val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    @Inject
    lateinit var database: ReferralDatabase
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    var token: String?

    init {
        (appContext as MyApp).component.inject(this)
        token = sharedPreferences.getString(TOKEN, "")
    }

    companion object {
        const val Progress = "Progress"
    }


    override fun doWork(): Result {
        Log.d("bugg", "sending the referral agaiiin ")
        val referralEntities: List<SmsReferralEntitiy> =
            database.daoAccess().getUnUploadedReferral()
        //setProgressAsync(Data.Builder().putInt(Progress, 0).build())
        referralEntities.forEach { f ->
            Log.d("bugg", "id: " + f.id)
            sendtoServer(f)
        }
        //setProgressAsync(Data.Builder().putInt(Progress, 100).build())

        // Indicate whether the task finished successfully with the Result
        Log.d("bugg", "task is finished")
        val x: HashMap<String, Boolean> = HashMap<String, Boolean>()
        x.put("finished", true)
        val ou: Data = Data.Builder().putAll(x as Map<String, Any>).build()

        return Result.success(Data.Builder().putBoolean("finished", true).build())
    }

    private fun sendtoServer(smsReferralEntitiy: SmsReferralEntitiy) {
        val json: JSONObject?
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
            Method.POST,
            SmsService.referralsServerUrl,
            json,
            Response.Listener { response: JSONObject? ->
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
                header[SmsService.AUTH] = "Bearer $token"
                return header
            }
        }
        val queue = Volley.newRequestQueue(appContext)
        queue.add(jsonObjectRequest)
    }

    fun updateDatabase(smsReferralEntitiy: SmsReferralEntitiy, isUploaded: Boolean) {
        smsReferralEntitiy.isUploaded = isUploaded
        smsReferralEntitiy.numberOfTriesUploaded++
        AsyncTask.execute {
            database.daoAccess().updateSmsReferral(smsReferralEntitiy)
        }
    }
}
