package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SmsRelayRepository(database: SmsRelayDatabase) {
    private var smsRelayDao: SmsRelayDao = database.smsRelayDao()
    var relayEntities: LiveData<List<SmsRelayEntity>> = smsRelayDao.getAllRelayEntities()

    fun insert(smsRelayEntity: SmsRelayEntity) {
        MainScope().launch(Dispatchers.IO) {
            smsRelayDao.insertSmsRelayEntity(smsRelayEntity)
        }
    }
}