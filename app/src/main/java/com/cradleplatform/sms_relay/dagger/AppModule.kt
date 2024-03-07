package com.cradleplatform.sms_relay.dagger

import androidx.multidex.MultiDexApplication
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(val application: MultiDexApplication) {

    @Provides
    @Singleton
    fun provideApplication() = application
}
