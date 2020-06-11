package com.cradle.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Main interface for interacting with the database for all classes.
 */
class ReferralRepository(database: ReferralDatabase) {
    private var referralDao: DaoAccess = database.daoAccess()
    var referrals: LiveData<List<SmsReferralEntitiy>>

    init {
        referrals = referralDao.getAllReferrals()
    }

    fun insert(smsReferralEntitiy: SmsReferralEntitiy) {
        MainScope().launch(IO) {
            referralDao.insertSmsReferral(smsReferralEntitiy)
        }
    }

    fun insertAll(smsReferralEntities: ArrayList<SmsReferralEntitiy>) {
        MainScope().launch(IO) {
            referralDao.insertAllReferral(smsReferralEntities)
        }
    }

    fun update(smsReferralEntitiy: SmsReferralEntitiy) {
        MainScope().launch(IO) { referralDao.updateSmsReferral(smsReferralEntitiy)
        }
    }

    fun delete(smsReferralEntitiy: SmsReferralEntitiy) {
        MainScope().launch(IO) {
            referralDao.deleteSmsReferral(smsReferralEntitiy)
        }
    }
    fun getAllUnUploadedReferrals(): List<SmsReferralEntitiy> {
        return referralDao.getUnUploadedReferral()
    }
}