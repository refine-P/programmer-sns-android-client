package com.example.programmersnsandroidclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.programmersnsandroidclient.model.SnsContent
import com.example.programmersnsandroidclient.model.SnsTimeline
import com.example.programmersnsandroidclient.model.TimelineState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class AbstractContentsViewModel(
    initialTimelineNumLimit: Int,
    private val incrementalTimelineNumLimit: Int,
    private val dispatcher: CoroutineDispatcher,
    willInitializeManually: Boolean,  // テスト用フラグ。テストでは手動で初期化関数を走らせたい。
) : ViewModel() {
    // TODO: 定数用のファイルを作って、そこにこれを移動した方が良いかも？
    companion object {
        const val DEFAULT_INITIAL_TIMELINE_NUM_LIMIT: Int = 50
        const val DEFAULT_INCREMENTAL_TINELINE_NUM_LIMIT: Int = 30
    }

    private var timelineNumLimit = initialTimelineNumLimit

    private val _timeline: MutableLiveData<SnsTimeline> = MutableLiveData()
    val timeline: LiveData<SnsTimeline> = _timeline

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        if (!willInitializeManually) init()
    }

    fun init(): Job {
        return loadTimeline(TimelineState.INIT)
    }

    fun refresh(): Job {
        return loadTimeline(TimelineState.REFRESH)
    }

    fun loadMore(): Job {
        return loadTimeline(TimelineState.LOAD_MORE)
    }

    protected abstract fun getShouldRefreshUserCache(state: TimelineState): Boolean
    protected abstract suspend fun fetchContents(
        timelineNumLimit: Int,
        shouldRefreshUserCache: Boolean
    ): List<SnsContent>?

    private fun loadTimeline(state: TimelineState): Job {
        val isDoing = when (state) {
            TimelineState.REFRESH -> _isRefreshing
            else -> _isLoading
        }
        isDoing.postValue(true)

        val shouldLoadMore = state == TimelineState.LOAD_MORE
        val shouldRefreshUserCache = getShouldRefreshUserCache(state)
        val numLimit = if (shouldLoadMore) {
            timelineNumLimit + incrementalTimelineNumLimit
        } else {
            timelineNumLimit
        }
        return viewModelScope.launch(dispatcher) {
            fetchContents(numLimit, shouldRefreshUserCache)?.let {
                _timeline.postValue(SnsTimeline(it, state))
                if (shouldLoadMore) timelineNumLimit = numLimit
            }
            isDoing.postValue(false)
        }
    }
}