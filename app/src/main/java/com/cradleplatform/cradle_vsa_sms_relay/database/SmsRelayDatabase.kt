package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cradleplatform.cradle_vsa_sms_relay.dao.SmsRelayDao
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.RelayRequestDataListConverter
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.RelayResponseDataListConverter
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.SmsListConverter
import com.cradleplatform.cradle_vsa_sms_relay.type_converters.TimeStampListConverter

/**
 * database that stores data on all sms transactions that were initiated
 */

@Database(entities = [RelayRequest::class], version = 9, exportSchema = false)
@TypeConverters(SmsListConverter::class, TimeStampListConverter::class, RelayResponseDataListConverter::class, RelayRequestDataListConverter::class)
abstract class SmsRelayDatabase : RoomDatabase() {
    abstract fun smsRelayDao(): SmsRelayDao
}
