package com.example.programmersnsandroidclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.programmersnsandroidclient.model.SnsContent
import com.example.programmersnsandroidclient.model.SnsRepository
import com.example.programmersnsandroidclient.model.TimelineState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class UserContentsViewModel(
    private val snsRepository: SnsRepository,
    private val targetUserId: String,
    initialTimelineNumLimit: Int,
    incrementalTimelineNumLimit: Int,
    dispatcher: CoroutineDispatcher,
) : AbstractContentsViewModel(initialTimelineNumLimit, incrementalTimelineNumLimit, dispatcher) {
    @AssistedInject
    constructor(
        snsRepository: SnsRepository,
        @Assisted targetUserId: String,
    ) : this(
        snsRepository,
        targetUserId,
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
        return snsRepository.fetchUserContents(
            targetUserId,
            timelineNumLimit,
            shouldRefreshUserCache
        )
    }

    @AssistedFactory
    interface ViewModelAssistedFactory {
        fun create(
            targetUserId: String
        ): UserContentsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: ViewModelAssistedFactory,
            targetUserId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(
                modelClass: Class<T>
            ): T {
                @Suppress("UNCHECKED_CAST")
                return assistedFactory.create(targetUserId) as T
            }
        }
    }
}
