package com.cradleplatform.smsrelay.dagger

import com.cradleplatform.smsrelay.activities.LauncherActivity
import com.cradleplatform.cradle_vsa_sms_relay.activities.MainActivity
import com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver.MessageReceiver
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayDatabase
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayRepository
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.cradle_vsa_sms_relay.network.NetworkManager
import com.cradleplatform.cradle_vsa_sms_relay.service.SmsService
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import com.cradleplatform.cradle_vsa_sms_relay.utilities.UploadReferralWorker
import com.cradleplatform.cradle_vsa_sms_relay.view_model.ReferralViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, DataModule::class])
interface AppComponent {

    fun inject(app: MyApp)
    fun inject(smsService: SmsService)
    fun inject(activity: MainActivity)
    fun inject(worker: UploadReferralWorker)
    fun inject(launcherActivity: LauncherActivity)
    fun inject(referralViewModel: ReferralViewModel)
    fun inject(repository: ReferralRepository)
    fun inject(messageReceiver: MessageReceiver)
    fun inject(networkManager: NetworkManager)
    fun inject(smsFormatter: SMSFormatter)
    fun inject(smsRelayRepository: SmsRelayRepository)
}
