package com.example.programmersnsandroidclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.programmersnsandroidclient.model.SnsRepository
import com.example.programmersnsandroidclient.model.SnsUser
import com.hadilq.liveevent.LiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnsViewModel(
    private val snsRepository: SnsRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    @Inject
    constructor(snsRepository: SnsRepository) : this(
        snsRepository,
        Dispatchers.IO
    )

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

    private fun updateCurrentUser(userId: String) {
        viewModelScope.launch(dispatcher) {
            snsRepository.fetchUser(userId)?.let {
                _currentUser.postValue(it)
            }
        }
    }
}
