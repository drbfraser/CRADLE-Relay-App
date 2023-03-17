package com.cradleplatform.smsrelay.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsReferralEntity

/**
 * Access database through [ReferralRepository] for proper use
 */
@Database(entities = [SmsReferralEntity::class], version = 2, exportSchema = false)
abstract class ReferralDatabase : RoomDatabase() {

    abstract fun daoAccess(): DaoAccess
}
