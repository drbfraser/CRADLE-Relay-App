package com.cradle.cradle_vsa_sms_relay

import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy

interface MultiMessageListener {
    fun messageMapRecieved(smsReferralList: List<SmsReferralEntitiy>)

}