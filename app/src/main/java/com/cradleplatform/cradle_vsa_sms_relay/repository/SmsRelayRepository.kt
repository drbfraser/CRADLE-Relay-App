package com.cradleplatform.cradle_vsa_sms_relay.repository

import androidx.lifecycle.LiveData
import com.cradleplatform.cradle_vsa_sms_relay.dao.SmsRelayDao
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsRelayDatabase
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * class to access the DAO interface for SmsRelayEntity objects
 */

class SmsRelayRepository(database: SmsRelayDatabase) {
    private var smsRelayDao: SmsRelayDao = database.smsRelayDao()
    var relayEntities: LiveData<List<SmsRelayEntity>> = smsRelayDao.getAllSmsRelayEntities()

    fun insert(smsRelayEntity: SmsRelayEntity) {
        MainScope().launch(Dispatchers.IO) {
            smsRelayDao.insertSmsRelayEntity(smsRelayEntity)
        }
    }

    fun insertBlocking(smsRelayEntity: SmsRelayEntity) {
        runBlocking {
            MainScope().launch(Dispatchers.IO) {
                smsRelayDao.insertSmsRelayEntity(smsRelayEntity)
            }
        }
    }

    fun getRelayBlocking(id: String): SmsRelayEntity? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                smsRelayDao.getRelayEntity(id)
            }
        }
    }

    fun getRelayLiveData(id: String): LiveData<SmsRelayEntity>? {
        return  smsRelayDao.getRelayEntityLiveData(id)
    }

    fun updateBlocking(smsRelayEntity: SmsRelayEntity) {
        runBlocking {
            MainScope().launch(Dispatchers.IO) {
                smsRelayDao.updateSmsRelayEntity(smsRelayEntity)
            }
        }
    }

    fun update(smsRelayEntity: SmsRelayEntity) {
        MainScope().launch(Dispatchers.IO) {
            smsRelayDao.updateSmsRelayEntity(smsRelayEntity)
        }
    }
}
