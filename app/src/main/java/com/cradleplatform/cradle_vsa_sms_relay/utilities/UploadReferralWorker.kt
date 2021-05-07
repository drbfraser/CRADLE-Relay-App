package com.cradleplatform.smsrelay.utilities

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cradleplatform.smsrelay.dagger.MyApp
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.smsrelay.database.SmsReferralEntity
import com.cradleplatform.smsrelay.network.Failure
import com.cradleplatform.smsrelay.network.NetworkManager
import com.cradleplatform.smsrelay.network.Success
import com.cradleplatform.smsrelay.network.VolleyRequests
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
class UploadReferralWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    @Inject
    lateinit var referralRepository: ReferralRepository

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var networkManager: NetworkManager

    init {
        (appContext as MyApp).component.inject(this)
    }

    override fun doWork(): Result {
        val referralEntities: List<SmsReferralEntity> =
            referralRepository.getAllUnUploadedReferrals()
        // setProgressAsync(Data.Builder().putInt(Progress, 0).build())
        referralEntities.forEach { f ->
            sendToServer(f)
        }
        // setProgressAsync(Data.Builder().putInt(Progress, 100).build())

        // Indicate whether the task finished successfully with the Result
        val x: HashMap<String, Boolean> = HashMap<String, Boolean>()
        x.put("finished", true)
        val ou: Data = Data.Builder().putAll(x as Map<String, Any>).build()

        return Result.success(Data.Builder().putBoolean("finished", true).build())
    }

    /**
     * uploads [smsReferralEntity] to the server
     * updates the status of the upload to the database.
     * todo figure out a way to get reference to the service and call service's sendToServer()
     */
    private fun sendToServer(smsReferralEntity: SmsReferralEntity) {
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

    private fun updateDatabase(smsReferralEntity: SmsReferralEntity, isUploaded: Boolean) {
        smsReferralEntity.isUploaded = isUploaded
        smsReferralEntity.numberOfTriesUploaded++
        AsyncTask.execute {
            referralRepository.update(smsReferralEntity)
        }
    }
}
