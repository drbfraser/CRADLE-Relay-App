package com.cradleplatform.cradle_vsa_sms_relay.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsSenderRepository (database: SmsSenderDatabase) {
    private var smsSenderDAO: SmsSenderDao = database.smsSenderDAO()

    fun insert(smsSenderEntity: SmsSenderEntity) {
        MainScope().launch(Dispatchers.IO) {
            smsSenderDAO.insertSmsSender(smsSenderEntity)
        }
    }

    suspend fun getSmsSenderEntity(id: String): SmsSenderEntity{
        return withContext(Dispatchers.IO){
            smsSenderDAO.getSenderEntity(id)
        }
    }
}