package com.example.cradle_vsa_sms_relay

import androidx.work.WorkInfo
import com.example.cradle_vsa_sms_relay.database.SmsReferralEntitiy

interface SingleMessageListener {
    fun newMessageReceived()
}
interface ReuploadReferralListener{
    fun onReuploadReferral(workInfo: WorkInfo);
}

interface MultiMessageListener {
    fun messageMapRecieved(smsReferralList: ArrayList<SmsReferralEntitiy>)

}