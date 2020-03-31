package com.example.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cradle_vsa_sms_relay.Sms
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

@Entity
data class SmsReferralEntitiy(
    @PrimaryKey
    val id: String,
    val jsonData: String,
    //unix stamp
    val timeRecieved: Int,
    var isUploaded: Boolean,
    val phoneNumber: String,
    val numberOfTriesUploaded: Int
):Serializable {


    companion object {
        fun fromJson(jsonString:String): SmsReferralEntitiy {
            try {
                val jsonObj = JSONObject(jsonString)
                val messageBody: String = jsonObj.getString("messageBody")
                val address:String = jsonObj.getString("address")
                val id:String = jsonObj.getString("id");
                val timeRecieved:Int = jsonObj.getInt("timeReceived");
                val numTries:Int = jsonObj.getInt("tries")
                return SmsReferralEntitiy(id,messageBody,timeRecieved,false,address,numTries)
            } catch (e: JSONException){
                return SmsReferralEntitiy("null",jsonString,0,false,"null",0)
            }
        }
    }
}