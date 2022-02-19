package com.example.programmersnsandroidclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.programmersnsandroidclient.model.SnsRepository
import com.example.programmersnsandroidclient.model.SnsTimeline
import com.example.programmersnsandroidclient.model.SnsUser
import com.example.programmersnsandroidclient.model.TimelineState
import com.hadilq.liveevent.LiveEvent
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

    private val _timeline: MutableLiveData<SnsTimeline> = MutableLiveData()
    val timeline: LiveData<SnsTimeline> = _timeline

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _updateSuccessful = LiveEvent<Boolean>()
    val updateSuccessful: LiveData<Boolean> = _updateSuccessful

    private val _sendSuccessful = LiveEvent<Boolean>()
    val sendSuccessful: LiveData<Boolean> = _sendSuccessful

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
            val isSuccessful = snsRepository.sendSnsPost(content)
            _sendSuccessful.postValue(isSuccessful)
        }
    }

    fun updateUserProfile(name: String, description: String) {
        viewModelScope.launch(dispatcher) {
            val userId = snsRepository.updateUser(name, description)
            val isSuccessful = userId != null
            _updateSuccessful.postValue(isSuccessful)
            if (userId != null) {  // ここを isSuccessful に置き換えるとエラーが出る
                _currentUser.postValue(SnsUser(userId, name, description))
                snsRepository.storeCurrentUserId(userId)
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
                _timeline.postValue(SnsTimeline(it, state))
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
