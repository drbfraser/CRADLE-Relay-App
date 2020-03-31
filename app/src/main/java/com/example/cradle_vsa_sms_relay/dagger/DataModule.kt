package com.example.cradle_vsa_sms_relay.dagger

import android.app.Application
import com.example.cradle_vsa_sms_relay.Sms
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule  {
    @Provides
    @Singleton
    fun getSms(app:Application):Sms{
        return Sms("d","d")
    }

}