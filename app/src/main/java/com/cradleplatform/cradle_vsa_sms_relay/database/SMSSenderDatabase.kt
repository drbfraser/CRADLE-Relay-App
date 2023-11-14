package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cradleplatform.smsrelay.database.DaoAccess
import com.cradleplatform.smsrelay.database.ReferralRepository

/**
 * Access database through [SMSSenderRepository] for proper use
 */
@Database(entities = [SMSSenderEntity::class], version = 1, exportSchema = false)
abstract class SMSSenderDatabase : RoomDatabase() {

    abstract fun smsSenderDAO(): SMSSenderDao
}