package com.cradleplatform.cradle_vsa_sms_relay.repository

import androidx.lifecycle.LiveData
import com.cradleplatform.cradle_vsa_sms_relay.dao.SmsRelayDao
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayDatabase
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestResult
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestPhase
import javax.inject.Inject

/**
 * class to access the DAO interface for SmsRelayEntity objects
 */

class SmsRelayRepository @Inject constructor (database: SmsRelayDatabase) {
    private var smsRelayDao: SmsRelayDao = database.smsRelayDao()
    var relayRequests: LiveData<List<RelayRequest>> = smsRelayDao.getAllRelayRequests()

    fun insertRelayRequest(relayRequest: RelayRequest) {
        smsRelayDao.insertRelayRequest(relayRequest)
    }

    fun markRelayRequestError(relayRequest: RelayRequest, errorMessage: String) {
        relayRequest.requestResult = RelayRequestResult.ERROR
        relayRequest.errorMessage = errorMessage

        smsRelayDao.updateRelayRequest(relayRequest)
    }

    fun markRelayRequestSuccess(relayRequest: RelayRequest) {
        relayRequest.requestResult = RelayRequestResult.OK
        relayRequest.requestPhase = RelayRequestPhase.COMPLETE

        smsRelayDao.updateRelayRequest(relayRequest)
    }
    fun getRelayRequestLiveData(requestId: Int, phoneNumber: String): LiveData<RelayRequest>? {
        return smsRelayDao.getRelayRequestLiveData(requestId, phoneNumber)
    }

    fun updateRelayRequest(relayRequest: RelayRequest) {
        smsRelayDao.updateRelayRequest(relayRequest)
    }

    fun updateRequestPhase(relayRequest: RelayRequest, status: RelayRequestPhase) {
        relayRequest.requestPhase = status

        updateRelayRequest(relayRequest)
    }

    fun terminateAllActiveRequests() {
        smsRelayDao.terminateAllActiveRequests(terminateReason = "Relay app restarted")
    }
}
