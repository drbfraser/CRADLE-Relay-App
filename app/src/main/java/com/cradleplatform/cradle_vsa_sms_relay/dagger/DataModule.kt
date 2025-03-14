package com.cradleplatform.cradle_vsa_sms_relay.dagger

import android.content.SharedPreferences
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayDatabase
import com.cradleplatform.cradle_vsa_sms_relay.managers.LoginManager
import com.cradleplatform.cradle_vsa_sms_relay.model.Settings
import com.cradleplatform.cradle_vsa_sms_relay.model.UrlManager
import com.cradleplatform.cradle_vsa_sms_relay.network.Http
import com.cradleplatform.cradle_vsa_sms_relay.network.RestApi
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.SmsListConverter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {

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

    @Singleton
    @Provides
    fun provideStringListConverter(): SmsListConverter {
        return SmsListConverter()
    }

    @Provides
    @Singleton
    fun provideSettings(
        sharedPreferences: SharedPreferences,
        context: MultiDexApplication
    ) = Settings(sharedPreferences, context)

    @Provides
    @Singleton
    fun provideUrlManager(settings: Settings) = UrlManager(settings)

    @Provides
    @Singleton
    fun provideHttp(
        sharedPreferences: SharedPreferences
    ): Http = Http(sharedPreferences)

    @Provides
    @Singleton
    fun provideRestApi(
        sharedPreferences: SharedPreferences,
        urlManager: UrlManager,
        http: Http
    ): RestApi = RestApi(sharedPreferences, urlManager, http)

    @Provides
    @Singleton
    fun provideLoginManager(
        restApi: RestApi,
        sharedPreferences: SharedPreferences
    ): LoginManager = LoginManager(restApi, sharedPreferences)
}
