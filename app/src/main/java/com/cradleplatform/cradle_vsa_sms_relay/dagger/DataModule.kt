package com.cradleplatform.cradle_vsa_sms_relay.dagger

import android.content.SharedPreferences
import android.util.Log
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayDatabase
import com.cradleplatform.cradle_vsa_sms_relay.network.NetworkManager
import com.cradleplatform.cradle_vsa_sms_relay.network.VolleyRequests
import com.cradleplatform.cradle_vsa_sms_relay.repository.HttpsRequestRepository
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.SmsListConverter
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {
    val TAG = "DataModule"

    @Provides
    @Singleton
    fun getSmsDatabase(app: MultiDexApplication): SmsRelayDatabase {
        return Room.databaseBuilder(
            app.applicationContext,
            SmsRelayDatabase::class.java,
            "relay-DB"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun getSharedPref(app: MultiDexApplication): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(app)
    }

    @Provides
    @Singleton
    fun getSmsRelayRepository(database: SmsRelayDatabase): SmsRelayRepository {
        return SmsRelayRepository(database)
    }

    @Provides
    @Singleton
    fun getNetworkManager(app: MultiDexApplication): NetworkManager {
        return NetworkManager(app)
    }

    @Provides
    @Singleton
    fun getHttpsRequestRepository(
        sharedPreference: SharedPreferences,
        smsFormatter: SMSFormatter,
        smsRelayRepository: SmsRelayRepository
    ): HttpsRequestRepository {
        val token = sharedPreference.getString(VolleyRequests.TOKEN, "") ?: ""
        val protocol = if(sharedPreference.getBoolean("key_server_use_https",true)){
            "https://"
        } else {
            "http://"
        }
        val hostname = sharedPreference.getString("key_server_hostname","cradleplatform.com")
        if (hostname == null) {
            Log.wtf(TAG, "Network hostname was null")
            throw NullPointerException()
        }

        val port = sharedPreference.getString("key_server_port","5000")
        val defaultBaseUrl = "http://10.0.2.2:5000/"
        val constructedUrl = "$protocol$hostname/$port/"
        Log.d(TAG, "this is url $constructedUrl")
        val baseUrl = sharedPreference.getString("base_url", constructedUrl) ?: constructedUrl
        return HttpsRequestRepository(token, smsFormatter, smsRelayRepository, baseUrl)
    }

    @Provides
    @Singleton
    fun getSMSFormatter(): SMSFormatter {
        return SMSFormatter()
    }

    @Singleton
    @Provides
    fun provideStringListConverter(): SmsListConverter {
        return SmsListConverter()
    }
}
