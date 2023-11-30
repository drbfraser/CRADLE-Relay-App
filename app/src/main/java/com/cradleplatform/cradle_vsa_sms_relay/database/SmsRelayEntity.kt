package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class SmsRelayEntity(
    @PrimaryKey
    val id: String,
    val numFragmentsReceived: Int,
    val totalFragments: Int,
    val fragmentsReceived: ArrayList<String>,
    val timeReceived: Long
) : Serializable, Comparable<SmsRelayEntity> {

    override fun compareTo(other: SmsRelayEntity): Int {
        return (this.timeReceived - other.timeReceived ).toInt()
    }
}