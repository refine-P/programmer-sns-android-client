package com.example.programmersnsandroidclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.programmersnsandroidclient.sns.SnsRepository
import com.example.programmersnsandroidclient.sns.SnsTimeline
import com.example.programmersnsandroidclient.sns.TimelineState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: 後でリファクタリングする
class SnsUserContentsViewModel(
    private val snsRepository: SnsRepository = SnsRepository(),
    initialTimelineNumLimit: Int = 50,
    private val incrementalTimelineNumLimit: Int = 30,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private var timelineNumLimit = initialTimelineNumLimit

    private val _timeline: MutableLiveData<SnsTimeline> = MutableLiveData()
    val timeline: LiveData<SnsTimeline> = _timeline

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // TODO: viewmodelが引数を持てるようにする
    private lateinit var targetUserId: String

    fun init(userId: String?) {
        // userIdがnull = currentUserIdを指定
        targetUserId = userId ?: snsRepository.loadCurrentUserId()!!
        loadTimeline(TimelineState.INIT)
    }

    fun refresh() {
        loadTimeline(TimelineState.REFRESH)
    }

    fun loadMore() {
        loadTimeline(TimelineState.LOAD_MORE)
    }

    private fun loadTimeline(state: TimelineState) {
        val isDoing = when (state) {
            TimelineState.REFRESH -> _isRefreshing
            else -> _isLoading
        }
        isDoing.postValue(true)

        val shouldLoadMore = state == TimelineState.LOAD_MORE
        val shouldRefreshUserCache = when (state) {
            TimelineState.INIT, TimelineState.REFRESH -> true
            else -> false
        }
        viewModelScope.launch(dispatcher) {
            val numLimit = if (shouldLoadMore) {
                timelineNumLimit + incrementalTimelineNumLimit
            } else {
                timelineNumLimit
            }
            snsRepository.fetchUserContents(targetUserId, numLimit, shouldRefreshUserCache)?.let {
                _timeline.postValue(SnsTimeline(it, state))
                if (shouldLoadMore) timelineNumLimit = numLimit
            }
            isDoing.postValue(false)
        }
    }
}
