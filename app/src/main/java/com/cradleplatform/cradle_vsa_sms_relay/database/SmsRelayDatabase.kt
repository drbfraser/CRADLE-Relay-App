package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SmsRelayEntity::class], version = 3, exportSchema = false)
abstract class SmsRelayDatabase : RoomDatabase() {
    abstract fun smsRelayDao(): SmsRelayDao
}
