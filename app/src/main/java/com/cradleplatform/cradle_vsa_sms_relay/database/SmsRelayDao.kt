package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SmsRelayDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSmsRelayEntity(smsRelayEntity: SmsRelayEntity)

    @Query("SELECT * FROM SmsRelayEntity")
    fun getAllSmsRelayEntities(): LiveData<List<SmsRelayEntity>>

    @Query("SELECT * FROM SmsRelayEntity WHERE id = :id LIMIT 1")
    fun getReferral(id: String): SmsRelayEntity?

    @Update
    fun updateSmsRelayEntity(smsRelayEntity: SmsRelayEntity)
}
