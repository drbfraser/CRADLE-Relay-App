package com.example.cradle_vsa_sms_relay.dagger

import android.app.Application
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
        return Room.databaseBuilder(
            app.applicationContext, ReferralDatabase::class.java,
            "referral-DB"
        ).allowMainThreadQueries().build()
    }
}