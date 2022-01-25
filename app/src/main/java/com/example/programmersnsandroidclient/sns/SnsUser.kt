package com.example.programmersnsandroidclient.sns

data class SnsUser(
    val id: String = "",
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