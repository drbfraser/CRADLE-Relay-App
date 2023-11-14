package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Access database through [SmsSenderRepository] for proper use
 */
@Database(entities = [SmsSenderEntity::class], version = 1, exportSchema = false)
abstract class SmsSenderDatabase : RoomDatabase() {

    abstract fun smsSenderDAO(): SmsSenderDao
}