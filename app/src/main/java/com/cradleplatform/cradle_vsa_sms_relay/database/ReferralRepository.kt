package com.cradleplatform.smsrelay.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Main interface for interacting with the database for all classes.
 */
class ReferralRepository(database: ReferralDatabase) {
    private var referralDao: DaoAccess = database.daoAccess()
    var referrals: LiveData<List<SmsReferralEntity>>

    init {
        referrals = referralDao.getAllReferrals()
    }

    fun insert(smsReferralEntity: SmsReferralEntity) {
        MainScope().launch(IO) {
            referralDao.insertSmsReferral(smsReferralEntity)
        }
    }

    fun insertAll(smsReferralEntities: List<SmsReferralEntity>) {
        MainScope().launch(IO) {
            referralDao.insertAllReferral(smsReferralEntities)
        }
    }

    fun update(smsReferralEntity: SmsReferralEntity) {
        MainScope().launch(IO) {
            referralDao.updateSmsReferral(smsReferralEntity)
        }
    }

    fun delete(smsReferralEntity: SmsReferralEntity) {
        MainScope().launch(IO) {
            referralDao.deleteSmsReferral(smsReferralEntity)
        }
    }

    fun getAllUnUploadedReferrals(): List<SmsReferralEntity> {
        return referralDao.getUnUploadedReferral()
    }

    fun getAllUnUploadedLiveListReferral(): LiveData<List<SmsReferralEntity>> {
        return referralDao.getUnUploadedReferralLive()
    }
}
