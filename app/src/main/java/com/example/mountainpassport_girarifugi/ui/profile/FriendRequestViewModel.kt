package com.example.mountainpassport_girarifugi.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mountainpassport_girarifugi.data.repository.FriendRepository
import com.google.firebase.firestore.ListenerRegistration

class FriendRequestsViewModel : ViewModel() {

    private val friendRepository = FriendRepository()
    private var friendRequestsListener: ListenerRegistration? = null

    private val _friendRequests = MutableLiveData<List<FriendRequest>>()
    val friendRequests: LiveData<List<FriendRequest>> = _friendRequests

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _actionResult = MutableLiveData<String?>()
    val actionResult: LiveData<String?> = _actionResult

    init {
        startListeningForFriendRequests()
    }

    private fun startListeningForFriendRequests() {
        _isLoading.value = true

        friendRequestsListener = friendRepository.listenForFriendRequests { requests ->
            _friendRequests.value = requests
            _isLoading.value = false
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            friendRepository.acceptFriendRequest(requestId) { success, error ->
                _isLoading.value = false

                if (success) {
                    _actionResult.value = "Richiesta di amicizia accettata!"
                } else {
                    _error.value = error ?: "Errore nell'accettare la richiesta"
                }
            }
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            friendRepository.declineFriendRequest(requestId) { success, error ->
                _isLoading.value = false

                if (success) {
                    _actionResult.value = "Richiesta di amicizia rifiutata"
                } else {
                    _error.value = error ?: "Errore nel rifiutare la richiesta"
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearActionResult() {
        _actionResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        friendRequestsListener?.remove()
    }
}