package com.cradleplatform.sms_relay.dagger

import com.cradleplatform.sms_relay.activities.LauncherActivity
import com.cradleplatform.sms_relay.activities.MainActivity
import com.cradleplatform.sms_relay.broadcast_receiver.MessageReceiver
import com.cradleplatform.sms_relay.repository.SmsRelayRepository
import com.cradleplatform.sms_relay.network.NetworkManager
import com.cradleplatform.sms_relay.repository.HttpsRequestRepository
import com.cradleplatform.sms_relay.service.SmsService
import com.cradleplatform.sms_relay.utilities.SMSFormatter
import com.cradleplatform.sms_relay.view_model.SmsRelayViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, DataModule::class])
interface AppComponent {

    fun inject(app: MyApp)
    fun inject(smsService: SmsService)
    fun inject(activity: MainActivity)
    fun inject(launcherActivity: LauncherActivity)
    fun inject(messageReceiver: MessageReceiver)
    fun inject(networkManager: NetworkManager)
    fun inject(smsFormatter: SMSFormatter)
    fun inject(smsRelayRepository: SmsRelayRepository)
    fun inject(httpsRequestRepository: HttpsRequestRepository)
    fun inject(smsRelayViewModel: SmsRelayViewModel)
}
