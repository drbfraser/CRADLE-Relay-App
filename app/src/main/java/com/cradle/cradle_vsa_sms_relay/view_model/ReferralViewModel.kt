package com.cradle.cradle_vsa_sms_relay.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.ReferralDatabase
import com.cradle.cradle_vsa_sms_relay.database.ReferralRepository
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import javax.inject.Inject

class ReferralViewModel constructor(application: Application) :
    AndroidViewModel(application) {

    private val repository: ReferralRepository
    private val referrals: LiveData<List<SmsReferralEntitiy>>

    @Inject
    lateinit var database: ReferralDatabase

    fun insert(smsReferralEntitiy: SmsReferralEntitiy) {
        repository.insert(smsReferralEntitiy)
    }

    fun update(smsReferralEntitiy: SmsReferralEntitiy) {
        repository.update(smsReferralEntitiy)
    }

    fun delete(smsReferralEntitiy: SmsReferralEntitiy) {
        repository.delete(smsReferralEntitiy)
    }

    fun getAllReferrals(): LiveData<List<SmsReferralEntitiy>> {
        return repository.referrals
    }

    init {
        (application as MyApp).component.inject(this)
        repository = ReferralRepository(database)
        referrals = repository.referrals
    }
}