package com.example.cradle_vsa_sms_relay

import android.telephony.SmsMessage
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * base class for sms body
 */
class Sms: Serializable {
    public var messageBody:String = ""
    public var address:String = ""

    public var status:Int = 0;

    constructor(messageBody: String?, address:String?){
        if (messageBody != null) {
            this.messageBody = messageBody
        }
        if (address!=null){
            this.address = address
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
                val address:String = jsonObj.getString("address")
                return Sms(messageBody,address)
            } catch (e:JSONException){
                return Sms(jsonString, jsonString)
            }
        }
    }
}