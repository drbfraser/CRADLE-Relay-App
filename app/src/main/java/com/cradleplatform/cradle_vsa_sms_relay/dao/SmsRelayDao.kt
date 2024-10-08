package com.cradleplatform.cradle_vsa_sms_relay.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity

/**
 * Data Access Object Interface for accessing the relay-DB that stores SmsRelayEntity
 */

@Dao
interface SmsRelayDao {

    // Using replace strategy on conflict 544
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSmsRelayEntity(smsRelayEntity: SmsRelayEntity)

    @Query("SELECT * FROM SmsRelayEntity")
    fun getAllSmsRelayEntities(): LiveData<List<SmsRelayEntity>>

    @Query("SELECT * FROM SmsRelayEntity WHERE id = :id LIMIT 1")
    fun getRelayEntity(id: String): SmsRelayEntity?

    @Query("SELECT * FROM SmsRelayEntity WHERE id = :id LIMIT 1")
    fun getRelayEntityLiveData(id: String): LiveData<SmsRelayEntity>?

    @Update
    fun updateSmsRelayEntity(smsRelayEntity: SmsRelayEntity)
}
