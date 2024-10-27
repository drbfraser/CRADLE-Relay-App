package com.cradleplatform.cradle_vsa_sms_relay.type_converters

import androidx.room.TypeConverter
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * class to convert list of sms packets received from mobile/server to a string
 * class is required because room database does not support the storing of lists
 * the list is first converted to a JSON and then stringified
 * functions here are automatically  called by the room database
 * and we do not need to manually call for a conversion
 */

class RelayRequestDataListConverter {
    @TypeConverter
    fun fromString(value: String): MutableList<RelayRequestData> {
        val listType = object : TypeToken<MutableList<RelayRequestData>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(value: MutableList<RelayRequestData>): String {
        return Gson().toJson(value)
    }
}
