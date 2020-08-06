package com.cradle.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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


    @Query("SELECT * FROM SmsReferralEntity where numberOfTriesUploaded  == 0")
    fun getUnUploadedReferralLive(): LiveData<List<SmsReferralEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllReferral(referralList: List<SmsReferralEntity>)
}
