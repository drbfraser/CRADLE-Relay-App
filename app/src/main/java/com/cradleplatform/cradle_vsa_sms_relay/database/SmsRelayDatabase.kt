package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cradleplatform.smsrelay.database.DaoAccess

@Database(entities = [SmsRelayEntity::class], version = 2, exportSchema = false)
abstract class SmsRelayDatabase : RoomDatabase() {
    abstract fun smsRelayDao(): SmsRelayDao
}
