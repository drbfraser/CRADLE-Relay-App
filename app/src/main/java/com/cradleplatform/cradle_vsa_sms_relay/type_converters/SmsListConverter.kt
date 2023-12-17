package com.cradleplatform.cradle_vsa_sms_relay.type_converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * class to convert list of sms packets received from mobile/server to a string
 * class is required because room database does not support the storing of lists
 * the list is first converted to a JSON and then stringified
 * functions here are automatically  called by the room database
 * and we do not need to manually call for a conversion
 */

class SmsListConverter {
    @TypeConverter
    fun fromString(value: String?): MutableList<String>? {
        if (value == null) {
            return null
        }

        val listType = object : TypeToken<MutableList<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(value: MutableList<String>?): String? {
        return Gson().toJson(value)
    }
}
