package com.example.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SmsEntitiy(
    @PrimaryKey
    val id: String,
    val jsonData: String,
    val timeRecieved: Int,
    val isUploaded: Boolean,
    val phoneNumber: String,
    val numberOfTries: Int
)