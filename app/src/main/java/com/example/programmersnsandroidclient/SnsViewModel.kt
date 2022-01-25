package com.example.programmersnsandroidclient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.programmersnsandroidclient.sns.SnsContent
import com.example.programmersnsandroidclient.sns.SnsModel
import com.example.programmersnsandroidclient.sns.SnsUser
import kotlinx.coroutines.*

class SnsViewModel(
    private val snsModel: SnsModel = SnsModel(),
    initialTimelineNumLimit: Int = 50,
    private val incrementalTimelineNumLimit: Int = 30,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private var timelineNumLimit = initialTimelineNumLimit

    private val _timeline: MutableLiveData<List<SnsContent>> = MutableLiveData(emptyList())
    val timeline : LiveData<List<SnsContent>> = _timeline

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    val isLoading : LiveData<Boolean> = _isLoading

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    val isRefreshing : LiveData<Boolean> = _isRefreshing

    private val _currentUser: MutableLiveData<SnsUser> = MutableLiveData()
    val currentUser : LiveData<SnsUser> = _currentUser

    init {
        load(false, shouldLoadMore = false)
    }

    fun refresh() {
        load(true, shouldLoadMore = false)
    }

    fun loadMore() {
        load(false, shouldLoadMore = true)
    }

    fun updateCurrentUser(userId: String) {
        viewModelScope.launch(dispatcher) {
            snsModel.fetchUser(userId)?.let {
                _currentUser.postValue(it)
            }
        }
    }

    fun sendSnsPost(content: String) {
        viewModelScope.launch(dispatcher) {
            snsModel.sendSnsPost(content)
        }
    }

    fun updateUserSetting(name: String, description: String) {
        viewModelScope.launch(dispatcher) {
            snsModel.updateUser(name, description)?.let {
                _currentUser.postValue(SnsUser(it, name, description))
            }
        }
    }

    private fun load(shouldRefresh: Boolean, shouldLoadMore: Boolean) {
        val isDoing = if (shouldRefresh) _isRefreshing else _isLoading
        isDoing.postValue(true)
        viewModelScope.launch(dispatcher) {
            val numLimit = if (shouldLoadMore) {
                timelineNumLimit + incrementalTimelineNumLimit
            } else {
                timelineNumLimit
            }
            snsModel.fetchTimeline(numLimit, shouldRefresh)?.let {
                _timeline.postValue(it)
                if (shouldLoadMore) timelineNumLimit = numLimit
            }
            isDoing.postValue(false)
        }
    }
}