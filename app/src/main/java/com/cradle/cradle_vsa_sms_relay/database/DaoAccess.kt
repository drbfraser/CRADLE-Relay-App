package com.cradle.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DaoAccess {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSmsReferral(smsReferralEntitiy: SmsReferralEntitiy)

    @Update
    fun updateSmsReferral(smsReferralEntitiy: SmsReferralEntitiy)

    @Delete
    fun deleteSmsReferral(smsReferralEntitiy: SmsReferralEntitiy)

    @Query("SELECT * FROM SmsReferralEntitiy WHERE isUploaded == 0")
    fun getUnUploadedReferral(): LiveData<List<SmsReferralEntitiy>>

    @Query("SELECT * FROM SmsReferralEntitiy")
    fun getAllReferrals(): LiveData<List<SmsReferralEntitiy>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllReferral(referralList: ArrayList<SmsReferralEntitiy>)
}