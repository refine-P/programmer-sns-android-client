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
) : AbstractContentsViewModel(initialTimelineNumLimit, incrementalTimelineNumLimit, dispatcher) {
    @Inject
    constructor(snsRepository: SnsRepository) : this(
        snsRepository,
        DEFAULT_INITIAL_TIMELINE_NUM_LIMIT,
        DEFAULT_INCREMENTAL_TINELINE_NUM_LIMIT,
        Dispatchers.IO
    )

    override fun getShouldRefreshUserCache(state: TimelineState): Boolean {
        // ユーザーのIDがgivenなら、UserCacheにそのIDは存在してるはず
        return false
    }

    override suspend fun fetchContents(
        timelineNumLimit: Int,
        shouldRefreshUserCache: Boolean
    ): List<SnsContent>? {
        return snsRepository.fetchTimeline(timelineNumLimit, shouldRefreshUserCache)
    }
}