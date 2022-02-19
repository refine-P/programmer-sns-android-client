package com.example.programmersnsandroidclient.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUser(id: String): SnsUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(users: List<SnsUser>)
}