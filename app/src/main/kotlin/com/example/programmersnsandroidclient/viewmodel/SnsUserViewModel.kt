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
class SnsUserViewModel(
    private val snsRepository: SnsRepository,
    private val dispatcher: CoroutineDispatcher,
    willInitializeManually: Boolean,  // テスト用フラグ。テストでは手動で初期化関数を走らせたい。
) : ViewModel() {
    @Inject
    constructor(snsRepository: SnsRepository) : this(
        snsRepository,
        Dispatchers.IO,
        false
    )

    private val _updateSuccessful = LiveEvent<Boolean>()
    val updateSuccessful: LiveData<Boolean> = _updateSuccessful

    private val _currentUser: MutableLiveData<SnsUser> = MutableLiveData()
    val currentUser: LiveData<SnsUser> = _currentUser

    init {
        if (!willInitializeManually) init()
    }

    fun init() = viewModelScope.launch(dispatcher) {
        snsRepository.loadCurrentUserId()?.let {
            updateCurrentUser(it)
        }
    }

    fun updateUserProfile(name: String, description: String) = viewModelScope.launch(dispatcher) {
        val userId = snsRepository.updateUser(name, description)
        val isSuccessful = userId != null
        _updateSuccessful.postValue(isSuccessful)
        if (userId != null) {  // ここを isSuccessful に置き換えるとエラーが出る
            _currentUser.postValue(SnsUser(userId, name, description))
            snsRepository.storeCurrentUserId(userId)
        }
    }

    private suspend fun updateCurrentUser(userId: String) {
        snsRepository.fetchUser(userId)?.let {
            _currentUser.postValue(it)
        }
    }
}
