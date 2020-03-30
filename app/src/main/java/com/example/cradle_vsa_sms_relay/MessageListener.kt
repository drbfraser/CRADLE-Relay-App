package com.example.cradle_vsa_sms_relay


 interface MessageListener {

    fun messageMapRecieved(Sms:HashMap<String?,String?>)
     fun singleMessageRecieved(sms:Sms)
}