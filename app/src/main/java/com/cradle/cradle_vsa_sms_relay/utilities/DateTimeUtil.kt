package com.cradle.cradle_vsa_sms_relay.utilities

import java.text.SimpleDateFormat
import java.util.*

class DateTimeUtil {

    companion object {
        fun convertUnixToTimeString(unixTime: Long): String {
            val cal: Calendar = Calendar.getInstance()
            val timezone: TimeZone = cal.timeZone
            val simpleDateFormat = SimpleDateFormat("MMM dd, yyyy hh.mm aa")
            simpleDateFormat.timeZone = timezone
            val localTime =
                //unix time is in seconds, converting to milli
                simpleDateFormat.format(Date(unixTime))
            return localTime

        }
    }
}