package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cradleplatform.cradle_vsa_sms_relay.dao.SmsRelayDao
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.SmsListConverter
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.TimeStampListConverter

@Database(entities = [SmsRelayEntity::class], version = 6, exportSchema = false)
@TypeConverters(SmsListConverter::class, TimeStampListConverter::class)
abstract class SmsRelayDatabase : RoomDatabase() {
    abstract fun smsRelayDao(): SmsRelayDao
}
