package com.example.programmersnsandroidclient.model

data class SnsContent(
    val contentId: String,
    val userId: String,
    val userName: String,
    val content: String,
    val createdAt: String = ""
)

data class SnsContentInternal(
    val id: String,
    val text: String,
    val in_reply_to_user_id: String?,
    val in_reply_to_text_id: String?,
    val _user_id: String,
    val _created_at: String,
    val _updated_at: String
)
