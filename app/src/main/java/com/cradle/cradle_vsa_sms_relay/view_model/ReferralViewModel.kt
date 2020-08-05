package com.cradle.cradle_vsa_sms_relay.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.ReferralRepository
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntity
import javax.inject.Inject

class ReferralViewModel (application: Application) :
    AndroidViewModel(application) {

    @Inject
    lateinit var repository: ReferralRepository

    private val referrals: LiveData<List<SmsReferralEntity>>

    fun insert(smsReferralEntity: SmsReferralEntity) {
        repository.insert(smsReferralEntity)
    }

    fun update(smsReferralEntity: SmsReferralEntity) {
        repository.update(smsReferralEntity)
    }

    fun delete(smsReferralEntity: SmsReferralEntity) {
        repository.delete(smsReferralEntity)
    }

    fun getAllReferrals(): LiveData<List<SmsReferralEntity>> {
        return repository.referrals
    }

    init {
        (application as MyApp).component.inject(this)
        referrals = repository.referrals
    }
}
