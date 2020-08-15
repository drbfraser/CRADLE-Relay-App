package com.cradle.cradle_vsa_sms_relay.utilities

import org.json.JSONException
import org.json.JSONObject

object ReferralMessageUtil {
    val referralJsonKeys = mapOf(
        "0" to "patient",
        "1" to "patientId",
        "2" to "patientName",
        "3" to "dob",
        "4" to "patientAge",
        "5" to "gestationalAgeUnit",
        "6" to "gestationalAgeValue",
        "7" to "villageNumber",
        "8" to "patientSex",
        "9" to "zone",
        "10" to "isPregnant",
        "11" to "reading",
        "12" to "readingId",
        "13" to "dateLastSaved",
        "14" to "dateTimeTaken",
        "15" to "bpSystolic",
        "16" to "urineTests",
        "17" to "urineTestBlood",
        "18" to "urineTestPro",
        "19" to "urineTestLeuc",
        "20" to "urineTestGlu",
        "21" to "urineTestNit",
        "22" to "userId",
        "23" to "bpDiastolic",
        "24" to "heartRateBPM",
        "25" to "dateRecheckVitalsNeeded",
        "26" to "isFlaggedForFollowup",
        "27" to "symptoms",
        "28" to "comment",
        "29" to "healthFacilityName",
        "30" to "date",
        "31" to "referralId"
    )

    private const val REFERRAL_ID_KEY = "referralId"

    fun getReferralJsonFromMessage(message: String): String {
        val referralJsonObject: JSONObject
        try {
            referralJsonObject = JSONObject(message)
        } catch (e: JSONException) {
            return message
        }
        // remove referralId
        referralJsonObject.remove(REFERRAL_ID_KEY)

        // replace number keys with string keys
        // for now, we skipping it, todo work with mobile to get numbers encoding working again
//        var newMessage = referralJsonObject.toString()
//        for (key in referralJsonKeys.keys) {
//            newMessage = newMessage.replace("\"$key\":", "\"" + referralJsonKeys[key] + "\":")
//        }
//        return newMessage
        return referralJsonObject.toString()
    }

    fun getIdFromMessage(message: String): String {
        return try {
            val jsonObject = JSONObject(message)
            jsonObject.getString(REFERRAL_ID_KEY)
        } catch (e: JSONException) {
            ""
        }
    }
}
