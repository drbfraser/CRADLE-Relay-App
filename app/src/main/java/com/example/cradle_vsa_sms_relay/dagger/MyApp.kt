package com.example.cradle_vsa_sms_relay.dagger

import android.app.Application

class MyApp : Application() {

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
            .appModule(AppModule(this)).dataModule(DataModule()).build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }
}