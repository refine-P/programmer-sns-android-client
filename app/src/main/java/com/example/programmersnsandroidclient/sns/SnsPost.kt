package com.example.programmersnsandroidclient.sns

data class SnsPost(
    val text: String = "",
    val in_reply_to_user_id: String?,
    val in_reply_to_text_id: String?,
)
