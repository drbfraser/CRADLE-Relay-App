package com.cradleplatform.sms_relay.utilities

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class DateTimeUtil {

    companion object {
        fun convertUnixToTimeString(unixTime: Long): String {
            val cal: Calendar = Calendar.getInstance()
            val timezone: TimeZone = cal.timeZone
            val simpleDateFormat = SimpleDateFormat("MMM d, yyyy h:mm aa")
            simpleDateFormat.timeZone = timezone
            return simpleDateFormat.format(Date(unixTime))
        }
    }
}
