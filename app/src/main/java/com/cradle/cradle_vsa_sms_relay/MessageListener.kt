package com.cradle.cradle_vsa_sms_relay

import androidx.work.WorkInfo
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy

interface SingleMessageListener {
    fun newMessageReceived()
}

interface MultiMessageListener {
    fun messageMapRecieved(smsReferralList: ArrayList<SmsReferralEntitiy>)

}