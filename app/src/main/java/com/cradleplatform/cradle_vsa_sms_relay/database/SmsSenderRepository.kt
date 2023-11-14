package com.cradleplatform.cradle_vsa_sms_relay.database

import com.cradleplatform.smsrelay.database.DaoAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SmsSenderRepository (database: SMSSenderDatabase) {
    private var smsSenderDAO: SMSSenderDao = database.smsSenderDAO()

    fun insert(smsReferralEntity: SMSSenderEntity) {
        MainScope().launch(Dispatchers.IO) {
            smsSenderDAO.insertSmsSender(smsReferralEntity)
        }
    }
}