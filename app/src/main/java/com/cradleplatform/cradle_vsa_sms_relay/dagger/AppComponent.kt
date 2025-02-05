package com.cradleplatform.cradle_vsa_sms_relay.dagger

import com.cradleplatform.cradle_vsa_sms_relay.activities.LauncherActivity
import com.cradleplatform.cradle_vsa_sms_relay.activities.MainActivity
import com.cradleplatform.cradle_vsa_sms_relay.activities.SettingsActivity
import com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver.MessageReceiver
import com.cradleplatform.cradle_vsa_sms_relay.managers.LoginManager
import com.cradleplatform.cradle_vsa_sms_relay.network.Http
import com.cradleplatform.cradle_vsa_sms_relay.network.RestApi
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import com.cradleplatform.cradle_vsa_sms_relay.service.SmsService
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import com.cradleplatform.cradle_vsa_sms_relay.view_model.DetailsViewModel
import com.cradleplatform.cradle_vsa_sms_relay.view_model.SmsRelayViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, DataModule::class])
@Suppress("TooManyFunctions")
interface AppComponent {

    fun inject(app: MyApp)
    fun inject(smsService: SmsService)
    fun inject(activity: MainActivity)
    fun inject(launcherActivity: LauncherActivity)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(messageReceiver: MessageReceiver)
    fun inject(smsFormatter: SMSFormatter)
    fun inject(smsRelayRepository: SmsRelayRepository)
    fun inject(smsRelayViewModel: SmsRelayViewModel)
    fun inject(detailsViewModel: DetailsViewModel)
    fun inject(http: Http)
    fun inject(restApi: RestApi)
    fun inject(loginManager: LoginManager)
}
