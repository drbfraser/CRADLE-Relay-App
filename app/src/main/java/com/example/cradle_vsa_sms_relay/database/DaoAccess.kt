package com.example.cradle_vsa_sms_relay.database

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
    fun getUnUploadedReferral(): List<SmsReferralEntitiy>

    @Query("SELECT * FROM SmsReferralEntitiy")
    fun getAllReferrals(): List<SmsReferralEntitiy>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllReferral(referralList: ArrayList<SmsReferralEntitiy>)
}