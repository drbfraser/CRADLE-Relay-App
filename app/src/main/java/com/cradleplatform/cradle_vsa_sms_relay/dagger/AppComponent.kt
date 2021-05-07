package com.cradleplatform.smsrelay.dagger

import com.cradleplatform.smsrelay.activities.LauncherActivity
import com.cradleplatform.smsrelay.activities.MainActivity
import com.cradleplatform.smsrelay.broadcast_receiver.MessageReciever
import com.cradleplatform.smsrelay.database.ReferralRepository
import com.cradleplatform.smsrelay.network.NetworkManager
import com.cradleplatform.smsrelay.service.SmsService
import com.cradleplatform.smsrelay.utilities.UploadReferralWorker
import com.cradleplatform.smsrelay.view_model.ReferralViewModel
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
    fun inject(messageReciever: MessageReciever)
    fun inject(networkManager: NetworkManager)
}
