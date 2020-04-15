package com.example.cradle_vsa_sms_relay

import androidx.work.WorkInfo
import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy

interface SingleMessageListener {
    fun newMessageReceived()
}
interface RetryTimerListener{
    fun onRetryTimeChanged(long: WorkInfo);
}

interface MultiMessageListener {
    fun messageMapRecieved(smsReferralList: ArrayList<SmsReferralEntitiy>)

}