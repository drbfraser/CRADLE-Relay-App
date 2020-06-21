package com.cradle.cradle_vsa_sms_relay

import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntity

interface MultiMessageListener {
    fun messageMapReceived(smsReferralList: List<SmsReferralEntity>)
}