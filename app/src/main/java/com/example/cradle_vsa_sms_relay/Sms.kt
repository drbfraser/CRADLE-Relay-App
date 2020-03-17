package com.example.cradle_vsa_sms_relay

import android.telephony.SmsMessage
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * base class for sms body
 */
class Sms: Serializable {
    public var messageBody:String = ""
    public var status:Int = 0;

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

    companion object {
        fun fromJson(jsonString:String):Sms{
            try {
                val jsonObj = JSONObject(jsonString)
                val messageBody: String = jsonObj.getString("messageBody")
                return Sms(messageBody)
            } catch (e:JSONException){
                return Sms(jsonString)
            }
        }
    }
}