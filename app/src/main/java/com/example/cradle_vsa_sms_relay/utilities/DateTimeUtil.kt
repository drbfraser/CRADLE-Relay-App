package com.example.cradle_vsa_sms_relay.utilities

import android.util.Log
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*

class DateTimeUtil {

    companion object {
        fun convertUnixToTimeString(unixTime: Long): String {
            val cal: Calendar = Calendar.getInstance()
            val tz: TimeZone = cal.getTimeZone()

            Log.d("Time zone: ", tz.getDisplayName())

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            sdf.setTimeZone(tz)
            val localTime =
                sdf.format(Date(unixTime * 1000))
            return localTime

        }
    }
}