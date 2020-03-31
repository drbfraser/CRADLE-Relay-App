package com.example.cradle_vsa_sms_relay

import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy

interface SingleMessageListener {
    fun singleMessageRecieved(sms:SmsReferralEntitiy)
}

interface MultiMessageListener{
    fun messageMapRecieved(smsReferralList:ArrayList<SmsReferralEntitiy>)

}