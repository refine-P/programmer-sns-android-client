package com.example.programmersnsandroidclient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.programmersnsandroidclient.sns.SnsContent
import com.example.programmersnsandroidclient.sns.SnsRepository
import com.example.programmersnsandroidclient.sns.SnsUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnsViewModel(
    private val snsRepository: SnsRepository = SnsRepository(),
    initialTimelineNumLimit: Int = 50,
    private val incrementalTimelineNumLimit: Int = 30,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private var timelineNumLimit = initialTimelineNumLimit

    private val _timeline: MutableLiveData<List<SnsContent>> = MutableLiveData(emptyList())
    val timeline: LiveData<List<SnsContent>> = _timeline

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _currentUser: MutableLiveData<SnsUser> = MutableLiveData()
    val currentUser: LiveData<SnsUser> = _currentUser

    init {
        snsRepository.loadCurrentUserId()?.let {
            updateCurrentUser(it)
        }
        loadTimeline(TimelineState.INIT)
    }

    fun refresh() {
        loadTimeline(TimelineState.REFRESH)
    }

    fun loadMore() {
        loadTimeline(TimelineState.LOAD_MORE)
    }

    fun sendSnsPost(content: String) {
        viewModelScope.launch(dispatcher) {
            snsRepository.sendSnsPost(content)
        }
    }

    fun updateUserProfile(name: String, description: String) {
        viewModelScope.launch(dispatcher) {
            snsRepository.updateUser(name, description)?.let {
                _currentUser.postValue(SnsUser(it, name, description))
                snsRepository.storeCurrentUserId(it)
            }
        }
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
            snsRepository.fetchTimeline(numLimit, shouldRefreshUserCache)?.let {
                _timeline.postValue(it)
                if (shouldLoadMore) timelineNumLimit = numLimit
            }
            isDoing.postValue(false)
        }
    }

    private fun updateCurrentUser(userId: String) {
        viewModelScope.launch(dispatcher) {
            snsRepository.fetchUser(userId)?.let {
                _currentUser.postValue(it)
            }
        }
    }
}

enum class TimelineState {
    INIT,
    REFRESH,
    LOAD_MORE,
}