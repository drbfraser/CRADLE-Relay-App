package com.cradle.cradle_vsa_sms_relay.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cradle.cradle_vsa_sms_relay.database.ReferralDatabase
import com.cradle.cradle_vsa_sms_relay.database.ReferralRepository
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import javax.inject.Inject

class ReferralViewModel(application: Application) : AndroidViewModel(application) {
    private val repository:ReferralRepository
    private val referrals:LiveData<List<SmsReferralEntitiy>>
    @Inject
    lateinit var database: ReferralDatabase

    init {
        repository = ReferralRepository(database)
        referrals = repository.referrals
    }

    public fun insert(smsReferralEntitiy: SmsReferralEntitiy){
        repository.insert(smsReferralEntitiy)
    }
    public fun update(smsReferralEntitiy: SmsReferralEntitiy){
        repository.update(smsReferralEntitiy)
    }
    public fun delete(smsReferralEntitiy: SmsReferralEntitiy){
        repository.delete(smsReferralEntitiy)
    }
    public fun getAllReferrals(): LiveData<List<SmsReferralEntitiy>> {
        return repository.referrals
    }
}