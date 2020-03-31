package com.example.cradle_vsa_sms_relay.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SmsReferralEntitiy::class],version = 1)
abstract class MyDatabase:RoomDatabase() {

    abstract  fun daoAccess(): DaoAccess
}