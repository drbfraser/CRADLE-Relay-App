package com.cradleplatform.cradle_vsa_sms_relay.dagger

import androidx.multidex.MultiDexApplication

class MyApp : MultiDexApplication() {

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
            .appModule(AppModule(this)).dataModule(DataModule()).build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }
}
