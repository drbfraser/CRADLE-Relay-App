package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
interface SMSSenderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSmsSender(smsSenderEntity: SMSSenderEntity)

    @Update
    fun updateSmsSender(smsSenderEntity: SMSSenderEntity)

}