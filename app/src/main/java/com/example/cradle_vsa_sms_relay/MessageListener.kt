package com.example.cradle_vsa_sms_relay

import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy

interface SingleMessageListener {
    fun newMessageReceived()
}
interface RetryTimerListener{
    fun onRetryTimeChanged(long: Long);
}

interface MultiMessageListener {
    fun messageMapRecieved(smsReferralList: ArrayList<SmsReferralEntitiy>)

}