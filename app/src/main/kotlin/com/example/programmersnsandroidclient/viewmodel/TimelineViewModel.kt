package com.example.programmersnsandroidclient.viewmodel

import com.example.programmersnsandroidclient.model.SnsContent
import com.example.programmersnsandroidclient.model.SnsRepository
import com.example.programmersnsandroidclient.model.TimelineState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel(
    private val snsRepository: SnsRepository,
    initialTimelineNumLimit: Int,
    incrementalTimelineNumLimit: Int,
    dispatcher: CoroutineDispatcher,
    willInitializeManually: Boolean
) : AbstractContentsViewModel(
    initialTimelineNumLimit,
    incrementalTimelineNumLimit,
    dispatcher,
    willInitializeManually
) {
    @Inject
    constructor(snsRepository: SnsRepository) : this(
        snsRepository,
        DEFAULT_INITIAL_TIMELINE_NUM_LIMIT,
        DEFAULT_INCREMENTAL_TINELINE_NUM_LIMIT,
        Dispatchers.IO,
        false
    )

    override fun getShouldRefreshUserCache(state: TimelineState): Boolean {
        return when (state) {
            TimelineState.INIT, TimelineState.REFRESH -> true
            else -> false
        }
    }

    override suspend fun fetchContents(
        timelineNumLimit: Int,
        shouldRefreshUserCache: Boolean
    ): List<SnsContent>? {
        return snsRepository.fetchTimeline(timelineNumLimit, shouldRefreshUserCache)
    }
}