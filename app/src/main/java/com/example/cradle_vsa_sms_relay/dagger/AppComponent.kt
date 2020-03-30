package com.example.cradle_vsa_sms_relay.dagger

import com.example.cradle_vsa_sms_relay.SmsService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(smsService: SmsService)
}