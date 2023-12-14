package com.cradleplatform.smsrelay.dagger

import android.content.SharedPreferences
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.SmsListConverter
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayDatabase
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import com.cradleplatform.smsrelay.database.ReferralDatabase
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.cradle_vsa_sms_relay.network.NetworkManager
import com.cradleplatform.cradle_vsa_sms_relay.repository.HttpsRequestRepository
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import com.cradleplatform.smsrelay.network.VolleyRequests
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    fun getDatabase(app: MultiDexApplication): ReferralDatabase {
        // todo create a migration class
        return Room.databaseBuilder(
            app.applicationContext, ReferralDatabase::class.java,
            "referral-DB"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun getSmsDatabase(app: MultiDexApplication): SmsRelayDatabase {
        return Room.databaseBuilder(
            app.applicationContext, SmsRelayDatabase::class.java,
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
    fun getReferralRepository(database: ReferralDatabase): ReferralRepository {
        return ReferralRepository(database)
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
        return HttpsRequestRepository(token, smsFormatter, smsRelayRepository)
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
