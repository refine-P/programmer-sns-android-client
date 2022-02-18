package com.example.programmersnsandroidclient.sns

data class SnsTimeline(
    val contents: List<SnsContent>,
    val state: TimelineState,
)

enum class TimelineState {
    INIT,
    REFRESH,
    LOAD_MORE,
}