package com.cradle.cradle_vsa_sms_relay.dagger

import com.cradle.cradle_vsa_sms_relay.activities.LauncherActivity
import com.cradle.cradle_vsa_sms_relay.activities.MainActivity
import com.cradle.cradle_vsa_sms_relay.database.ReferralRepository
import com.cradle.cradle_vsa_sms_relay.service.SmsService
import com.cradle.cradle_vsa_sms_relay.utilities.UploadReferralWorker
import com.cradle.cradle_vsa_sms_relay.view_model.ReferralViewModel
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
}
