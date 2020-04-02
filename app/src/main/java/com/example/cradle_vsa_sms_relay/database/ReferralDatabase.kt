package com.example.cradle_vsa_sms_relay.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SmsReferralEntitiy::class], version = 1)
abstract class ReferralDatabase : RoomDatabase() {

    abstract fun daoAccess(): DaoAccess
}