package com.cradleplatform.cradle_vsa_sms_relay.utilities

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateTimeUtil {
    companion object {
        const val MS_IN_A_SEC = 1000
        fun convertUnixToTimeString(unixTime: Long): String {
            val cal: Calendar = Calendar.getInstance()
            val timezone: TimeZone = cal.timeZone
            val simpleDateFormat = SimpleDateFormat("MMM d, yyyy h:mm aa", Locale.getDefault())
            simpleDateFormat.timeZone = timezone
            return simpleDateFormat.format(Date(unixTime))
        }
    }
}
