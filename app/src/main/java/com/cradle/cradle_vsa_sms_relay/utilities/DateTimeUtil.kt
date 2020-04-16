package com.cradle.cradle_vsa_sms_relay.utilities

import java.text.SimpleDateFormat
import java.util.*

class DateTimeUtil {

    companion object {
        fun convertUnixToTimeString(unixTime: Long): String {
            val cal: Calendar = Calendar.getInstance()
            val timezone: TimeZone = cal.timeZone
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            simpleDateFormat.timeZone = timezone
            val localTime =
                //unix time is in seconds, converting to milli
                simpleDateFormat.format(Date(unixTime * 100))
            return localTime

        }
    }
}