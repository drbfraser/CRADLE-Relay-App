package com.cradleplatform.cradle_vsa_sms_relay.dagger

import android.content.SharedPreferences
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.cradleplatform.cradle_vsa_sms_relay.database.ReferralDatabase
import com.cradleplatform.cradle_vsa_sms_relay.database.ReferralRepository
import com.cradleplatform.cradle_vsa_sms_relay.network.NetworkManager
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
    fun getNetworkManager(app: MultiDexApplication): NetworkManager {
        return NetworkManager(app)
    }
}
