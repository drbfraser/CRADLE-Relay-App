package com.cradle.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

@Entity
data class SmsReferralEntitiy(
    @PrimaryKey
    val id: String,
    val jsonData: String?,
    //unix stamp
    val timeRecieved: Long,
    var isUploaded: Boolean,
    val phoneNumber: String?,
    var numberOfTriesUploaded: Int,
    var errorMessage:String
) : Serializable, Comparable<SmsReferralEntitiy> {


    companion object {
        fun fromJson(jsonString: String): SmsReferralEntitiy {
            return try {
                val jsonObj = JSONObject(jsonString)
                val messageBody: String = jsonObj.getString("messageBody")
                val address: String = jsonObj.getString("address")
                val id: String = jsonObj.getString("id")
                val timeRecieved: Long = jsonObj.getLong("timeReceived")
                val numTries: Int = jsonObj.getInt("tries")
                SmsReferralEntitiy(id, messageBody, timeRecieved, false, address, numTries,"")
            } catch (e: JSONException) {
                SmsReferralEntitiy("null", jsonString, 0, false, "null", 0,"")
            }
        }
    }

    override fun compareTo(other: SmsReferralEntitiy): Int {
        return (this.timeRecieved - other.timeRecieved).toInt()
    }
}