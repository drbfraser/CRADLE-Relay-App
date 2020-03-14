package com.example.cradle_vsa_sms_relay

import android.telephony.SmsMessage
import org.json.JSONObject
import java.io.Serializable

/**
 * base class for sms body
 */
class Sms: Serializable {
    var messageBody:String = ""

    constructor(messageBody: String?){
        if (messageBody != null) {
            this.messageBody = messageBody
        }
    }
    constructor(message: SmsMessage){
        this.messageBody = message.messageBody
    }

    fun toJson():JSONObject{
        val jsonObject = JSONObject()
        jsonObject.put("messageBody",messageBody);
        return jsonObject
    }
}