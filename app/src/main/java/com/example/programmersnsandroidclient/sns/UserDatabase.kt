package com.example.programmersnsandroidclient.sns

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SnsUser::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}