package com.example.cradle_vsa_sms_relay

interface SingleMessageListener {
    fun singleMessageRecieved(sms:Sms)
}

interface MultiMessageListener{
    fun messageMapRecieved(Sms:HashMap<String?,String?>)

}