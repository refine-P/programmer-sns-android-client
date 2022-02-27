package com.example.programmersnsandroidclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.programmersnsandroidclient.model.SnsRepository
import com.hadilq.liveevent.LiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SendSnsPostViewModel(
    private val snsRepository: SnsRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    @Inject
    constructor(snsRepository: SnsRepository) : this(
        snsRepository,
        Dispatchers.IO
    )

    private val _sendSuccessful = LiveEvent<Boolean>()
    val sendSuccessful: LiveData<Boolean> = _sendSuccessful

    fun sendSnsPost(content: String) = viewModelScope.launch(dispatcher) {
        val isSuccessful = snsRepository.sendSnsPost(content)
        _sendSuccessful.postValue(isSuccessful)
    }
}