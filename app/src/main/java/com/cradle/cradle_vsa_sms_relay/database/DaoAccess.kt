package com.cradle.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DaoAccess {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSmsReferral(smsReferralEntity: SmsReferralEntity)

    @Update
    fun updateSmsReferral(smsReferralEntity: SmsReferralEntity)

    @Delete
    fun deleteSmsReferral(smsReferralEntity: SmsReferralEntity)

    @Query("SELECT * FROM SmsReferralEntity WHERE isUploaded == 0")
    fun getUnUploadedReferral(): List<SmsReferralEntity>

    @Query("SELECT * FROM SmsReferralEntity")
    fun getAllReferrals(): LiveData<List<SmsReferralEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllReferral(referralList: List<SmsReferralEntity>)
}