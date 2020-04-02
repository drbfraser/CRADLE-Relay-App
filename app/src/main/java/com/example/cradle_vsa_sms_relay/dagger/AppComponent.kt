package com.example.cradle_vsa_sms_relay.dagger

import com.example.cradle_vsa_sms_relay.SmsService
import com.example.cradle_vsa_sms_relay.activities.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, DataModule::class])
interface AppComponent {

    fun inject(app: MyApp)
    fun inject(smsService: SmsService)
    fun inject(activity: MainActivity)
}