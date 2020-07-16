package com.cradle.cradle_vsa_sms_relay.utilities

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.ReferralRepository
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradle.cradle_vsa_sms_relay.service.SmsService
import com.cradle.cradle_vsa_sms_relay.service.SmsService.Companion.TOKEN
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import javax.inject.Inject
import org.json.JSONException
import org.json.JSONObject

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
    lateinit var referralRepository: ReferralRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    var token: String?

    init {
        (appContext as MyApp).component.inject(this)
        token = sharedPreferences.getString(TOKEN, "")
    }

    companion object {
        const val INTERNAL_SERVER_ERROR = 500
        const val CLIENT_ERROR_CODE = 400
    }

    override fun doWork(): Result {
        val referralEntities: List<SmsReferralEntity> =
            referralRepository.getAllUnUploadedReferrals()
        // setProgressAsync(Data.Builder().putInt(Progress, 0).build())
        referralEntities.forEach { f ->
            sendtoServer(f)
        }
        // setProgressAsync(Data.Builder().putInt(Progress, 100).build())

        // Indicate whether the task finished successfully with the Result
        val x: HashMap<String, Boolean> = HashMap<String, Boolean>()
        x.put("finished", true)
        val ou: Data = Data.Builder().putAll(x as Map<String, Any>).build()

        return Result.success(Data.Builder().putBoolean("finished", true).build())
    }

    private fun sendtoServer(smsReferralEntity: SmsReferralEntity) {
        val json: JSONObject?
        try {
            json = JSONObject(smsReferralEntity.jsonData)
        } catch (e: JSONException) {
            smsReferralEntity.errorMessage = "Not a valid JSON format"
            updateDatabase(smsReferralEntity, false)
            e.printStackTrace()
            // no need to send it to the server, we know its not a valid json
            return
        }
        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            SmsService.referralsServerUrl,
            json,
            Response.Listener { response: JSONObject? ->
                updateDatabase(smsReferralEntity, true)
            },
            Response.ErrorListener { error: VolleyError ->
                var json: String? = ""
                try {
                    if (error.networkResponse != null) {
                        json = String(
                            error.networkResponse.data,
                            Charset.forName(HttpHeaderParser.parseCharset(error.networkResponse.headers))
                        )
                        smsReferralEntity.errorMessage = json.toString()
                    }
                } catch (e: UnsupportedEncodingException) {
                    smsReferralEntity.errorMessage =
                        "No clue whats going on, return message is null"
                    e.printStackTrace()
                }
                // giving back extra info based on status code
                if (error.networkResponse != null) {
                    if (error.networkResponse.statusCode >= INTERNAL_SERVER_ERROR) {
                        smsReferralEntity.errorMessage += " Please make sure referral has all the fields"
                    } else {
                        if (error.networkResponse.statusCode >= CLIENT_ERROR_CODE) {
                            smsReferralEntity.errorMessage += " Invalid request, make sure you have correct credentials"
                        }
                    }
                } else {
                    smsReferralEntity.errorMessage = "Unable to get error message"
                }
                updateDatabase(smsReferralEntity, false)
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

    fun updateDatabase(smsReferralEntity: SmsReferralEntity, isUploaded: Boolean) {
        smsReferralEntity.isUploaded = isUploaded
        smsReferralEntity.numberOfTriesUploaded++
        AsyncTask.execute {
            referralRepository.update(smsReferralEntity)
        }
    }
}
