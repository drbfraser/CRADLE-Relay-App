package com.cradleplatform.cradle_vsa_sms_relay.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SmsSenderRepository (database: SmsSenderDatabase) {
    private var smsSenderDAO: SmsSenderDao = database.smsSenderDAO()

    fun insert(smsReferralEntity: SmsSenderEntity) {
        MainScope().launch(Dispatchers.IO) {
            smsSenderDAO.insertSmsSender(smsReferralEntity)
        }
    }
}