package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SmsRelayDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSmsRelayEntity(smsRelayEntity: SmsRelayEntity)

    @Query("SELECT * FROM SmsRelayEntity")
    fun getAllRelayEntities(): LiveData<List<SmsRelayEntity>>
}