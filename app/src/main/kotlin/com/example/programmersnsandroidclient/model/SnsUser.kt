package com.example.programmersnsandroidclient.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class SnsUser(
    @PrimaryKey val id: String = "",
    val name: String,
    val description: String
)

data class UserId(
    val id: String
)

data class UserSetting(
    val name: String,
    val description: String
)