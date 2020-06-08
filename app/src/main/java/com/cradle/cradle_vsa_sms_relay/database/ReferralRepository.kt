package com.cradle.cradle_vsa_sms_relay.database

import android.app.Application
import androidx.lifecycle.LiveData

class ReferralRepository {
    private var referralDao:DaoAccess
    private var referrals:LiveData<List<SmsReferralEntitiy>>
    get() = field

    constructor(database: ReferralDatabase){
        referralDao = database.daoAccess()
        referrals = referralDao.getAllReferrals()
    }

    fun insert(smsReferralEntitiy: SmsReferralEntitiy){

    }
    fun update(smsReferralEntitiy: SmsReferralEntitiy){

    }
    fun delete(smsReferralEntitiy: SmsReferralEntitiy){

    }
}