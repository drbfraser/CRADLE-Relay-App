package com.cradleplatform.cradle_vsa_sms_relay.type_converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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