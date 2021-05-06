package com.cradleplatform.smsrelay.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Access database through [ReferralRepository] for proper use
 */
@Database(entities = [SmsReferralEntity::class], version = 1, exportSchema = false)
abstract class ReferralDatabase : RoomDatabase() {

    abstract fun daoAccess(): DaoAccess
}
