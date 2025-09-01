package com.example.mountainpassport_girarifugi.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.repository.FriendRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Data class che rappresenta l'utente
 */
data class AddFriendUser(
    val id: String,
    val name: String,
    val username: String,
    val points: Int,
    val refugesCount: Int,
    val avatarResource: Int,
    val isAlreadyFriend: Boolean = false,
    val isRequestSent: Boolean = false,
    val profileImageUrl: String? = null
)

class AddFriendsViewModel : ViewModel() {

    private val _searchResults = MutableLiveData<List<AddFriendUser>>()
    val searchResults: LiveData<List<AddFriendUser>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val friendRepository = FriendRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadDefaultUsers()
    }

    fun loadDefaultUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                searchUsersFromFirebase("")
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento utenti: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun addFriend(user: AddFriendUser) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendRepository.sendFriendRequest(user.id) { success, error ->
                    if (success) {
                        val currentList = _searchResults.value ?: emptyList()
                        val updatedList = currentList.map { currentUser ->
                            if (currentUser.id == user.id) {
                                currentUser.copy(isRequestSent = true)
                            } else {
                                currentUser
                            }
                        }
                        _searchResults.value = updatedList
                        _error.value = null
                    } else {
                        _error.value = error ?: "Errore nell'invio della richiesta"
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Errore nell'invio della richiesta di amicizia: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkFriendshipStatus(userId: String, currentUserId: String): Pair<Boolean, Boolean> {
        return try {
            val friendDoc = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(userId)
                .get()
                .await()

            val isAlreadyFriend = friendDoc.exists()

            val requestSnapshot = firestore.collection("friendRequests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val isRequestSent = !requestSnapshot.isEmpty

            Pair(isAlreadyFriend, isRequestSent)
        } catch (e: Exception) {
            android.util.Log.e("AddFriendsVM", "Error checking friendship status", e)
            Pair(false, false)
        }
    }

    fun searchUsersFromFirebase(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch

                firestore.collection("users")
                    .get()
                    .addOnSuccessListener { documents ->
                        viewModelScope.launch {
                            val users = mutableListOf<AddFriendUser>()

                            for (doc in documents) {
                                val user =
                                    doc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)

                                if (doc.id != currentUserId &&
                                    (query.isBlank() ||
                                            user.nome.contains(query, ignoreCase = true) ||
                                            user.cognome.contains(query, ignoreCase = true) ||
                                            user.nickname.contains(query, ignoreCase = true))
                                ) {

                                    val (isAlreadyFriend, isRequestSent) =
                                        checkFriendshipStatus(doc.id, currentUserId)

                                    val userStats = getUserStatsFromPoints(doc.id)

                                    val addFriendUser = AddFriendUser(
                                        id = doc.id,
                                        name = "${user.nome} ${user.cognome}".trim(),
                                        username = user.nickname,
                                        points = userStats.first,
                                        refugesCount = userStats.second,
                                        avatarResource = R.drawable.avatar_mario,
                                        isAlreadyFriend = isAlreadyFriend,
                                        isRequestSent = isRequestSent,
                                        profileImageUrl = user.profileImageUrl
                                    )

                                    users.add(addFriendUser)
                                }
                            }

                            _searchResults.value = users
                            _isLoading.value = false
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("AddFriendsVM", "Error loading users", e)
                        _error.value = "Errore nel caricamento utenti: ${e.message}"
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                android.util.Log.e("AddFriendsVM", "Exception in searchUsersFromFirebase", e)
                _error.value = "Errore nella ricerca: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun getUserStatsFromPoints(userId: String): Pair<Int, Int> {
        return try {
            val userStatsDoc = firestore.collection("user_points_stats")
                .document(userId)
                .get()
                .await()

            val userStats =
                userStatsDoc.toObject(com.example.mountainpassport_girarifugi.data.model.UserPointsStats::class.java)

            if (userStats != null) {
                Pair(userStats.totalPoints, userStats.totalVisits)
            } else {
                Pair(0, 0)
            }
        } catch (e: Exception) {
            android.util.Log.e("AddFriendsVM", "Error getting user stats", e)
            Pair(0, 0)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
