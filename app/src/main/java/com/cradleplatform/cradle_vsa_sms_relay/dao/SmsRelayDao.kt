package com.cradleplatform.cradle_vsa_sms_relay.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestResult

/**
 * Data Access Object Interface for accessing the relay-DB that stores SmsRelayEntity
 */

@Dao
interface SmsRelayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRelayRequest(relayRequest: RelayRequest): Long

    @Query("SELECT * FROM RelayRequest ORDER BY timeMsLastReceived DESC")
    fun getAllRelayRequests(): LiveData<List<RelayRequest>>

    @Query("SELECT * FROM RelayRequest WHERE requestId = :requestId AND phoneNumber = :phoneNumber LIMIT 1")
    fun getRelayRequestLiveData(requestId: Int, phoneNumber: String): LiveData<RelayRequest>?

    @Update
    fun updateRelayRequest(relayRequest: RelayRequest)

    @Query(
        """
        UPDATE RelayRequest 
        SET 
            errorMessage = :terminateReason,
            requestResult = '${RelayRequestResult.ERROR}'
        WHERE requestResult = '${RelayRequestResult.PENDING}'
        """
    )
    fun terminateAllActiveRequests(
        terminateReason: String,
    )
}
