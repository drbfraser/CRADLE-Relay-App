package com.example.cradle_vsa_sms_relay.dagger

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.example.cradle_vsa_sms_relay.database.ReferralDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    fun getDatabase(app: Application): ReferralDatabase {
        //todo dont allow main thread queries
        //todo create a migration class
        return Room.databaseBuilder(
            app.applicationContext, ReferralDatabase::class.java,
            "referral-DB"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun getSharedPref(app: Application):SharedPreferences{
        return PreferenceManager.getDefaultSharedPreferences(app)
    }
}