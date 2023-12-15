package com.cradleplatform.cradle_vsa_sms_relay.type_converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * class to convert list of timestamps to a string
 * class is required because room database does not support the storing of lists
 * the list is first converted to a JSON and then stringified
 * functions here are automatically called by the room database
 * there is no need to manually call for a conversion
 */

class TimeStampListConverter {
    @TypeConverter
    fun fromString(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<Long>): String {
        return Gson().toJson(list)
    }
}