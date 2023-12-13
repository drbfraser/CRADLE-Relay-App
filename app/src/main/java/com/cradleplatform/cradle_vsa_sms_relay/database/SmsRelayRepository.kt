package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SmsRelayRepository(database: SmsRelayDatabase) {
    private var smsRelayDao: SmsRelayDao = database.smsRelayDao()
    var relayEntities: LiveData<List<SmsRelayEntity>> = smsRelayDao.getAllSmsRelayEntities()

    fun insert(smsRelayEntity: SmsRelayEntity) {
        MainScope().launch(Dispatchers.IO) {
            smsRelayDao.insertSmsRelayEntity(smsRelayEntity)
        }
    }

    fun insertBlocking(smsRelayEntity: SmsRelayEntity){
        runBlocking {
            MainScope().launch(Dispatchers.IO) {
                smsRelayDao.insertSmsRelayEntity(smsRelayEntity)
            }
        }
    }

    fun getReferralBlocking(id: String): SmsRelayEntity? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                smsRelayDao.getReferral(id)
            }
        }
    }

    fun updateBlocking(smsRelayEntity: SmsRelayEntity){
        runBlocking {
            MainScope().launch(Dispatchers.IO) {
                smsRelayDao.updateSmsRelayEntity(smsRelayEntity)
            }
        }
    }
}
