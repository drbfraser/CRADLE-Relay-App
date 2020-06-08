package com.cradle.cradle_vsa_sms_relay.database

import android.app.Application
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ReferralRepository {
    private var referralDao:DaoAccess
    private var referrals:LiveData<List<SmsReferralEntitiy>>
    get() = field

    constructor(database: ReferralDatabase){
        referralDao = database.daoAccess()
        referrals = referralDao.getAllReferrals()
    }

    fun insert(smsReferralEntitiy: SmsReferralEntitiy){
        MainScope().launch(IO) {
            referralDao.insertSmsReferral(smsReferralEntitiy)
        }
    }
    fun insertAll(smsReferralEntities:ArrayList<SmsReferralEntitiy>){
        MainScope().launch(IO) {
            referralDao.insertAllReferral(smsReferralEntities)
        }
    }
    fun update(smsReferralEntitiy: SmsReferralEntitiy){
        MainScope().launch(IO) { referralDao.updateSmsReferral(smsReferralEntitiy) }

    }
    fun delete(smsReferralEntitiy: SmsReferralEntitiy){
        MainScope().launch(IO) {
            referralDao.deleteSmsReferral(smsReferralEntitiy)
        }
    }
}