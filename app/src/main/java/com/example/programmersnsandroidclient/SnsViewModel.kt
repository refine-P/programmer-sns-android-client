package com.example.programmersnsandroidclient

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.programmersnsandroidclient.sns.SnsContent
import com.example.programmersnsandroidclient.sns.SnsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnsViewModel(
    val timeline: MutableLiveData<List<SnsContent>> = MutableLiveData(emptyList()),
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(true),
    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
) : ViewModel() {
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        load(false)
    }

    fun refresh() {
        load(true)
    }

    fun loadMore() {
        isLoading.postValue(true)
        scope.launch {
            SnsModel.fetchTimelineMore()?.let {
                timeline.postValue(it)
            }
            isLoading.postValue(false)
        }
    }

    private fun load(forRefresh: Boolean) {
        val isDoing = if (forRefresh) isRefreshing else isLoading
        isDoing.postValue(true)
        scope.launch {
            SnsModel.fetchTimeline(forRefresh)?.let {
                timeline.postValue(it)
            }
            isDoing.postValue(false)
        }
    }
}