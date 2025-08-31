package com.example.mountainpassport_girarifugi.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mountainpassport_girarifugi.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

data class LeaderboardUser(
    val id: String,
    val name: String,
    val points: Int,
    val refugesCount: Int,
    val avatarResource: Int,
    val position: Int = 0,
    val profileImageUrl: String? = null
) {
    companion object {
        fun empty() = LeaderboardUser(
            id = "",
            name = "",
            points = 0,
            refugesCount = 0,
            avatarResource = R.drawable.avatar_mario
        )
    }
}

class LeaderboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData per la classifica generale
    private val _globalLeaderboard = MutableLiveData<List<LeaderboardUser>>()
    val globalLeaderboard: LiveData<List<LeaderboardUser>> = _globalLeaderboard

    // LiveData per la classifica degli amici
    private val _friendsLeaderboard = MutableLiveData<List<LeaderboardUser>>()
    val friendsLeaderboard: LiveData<List<LeaderboardUser>> = _friendsLeaderboard

    // LiveData per i gruppi (se necessario)
    private val _groupsLeaderboard = MutableLiveData<List<LeaderboardUser>>()
    val groupsLeaderboard: LiveData<List<LeaderboardUser>> = _groupsLeaderboard

    // Liste originali per la ricerca
    private var originalFriendsList: List<LeaderboardUser> = emptyList()
    private var originalGlobalList: List<LeaderboardUser> = emptyList()
    private var originalGroupsList: List<LeaderboardUser> = emptyList()

    // Stati di caricamento
    private val _isLoadingGlobal = MutableLiveData<Boolean>()
    val isLoadingGlobal: LiveData<Boolean> = _isLoadingGlobal

    private val _isLoadingFriends = MutableLiveData<Boolean>()
    val isLoadingFriends: LiveData<Boolean> = _isLoadingFriends

    // Errori
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    companion object {
        private const val TAG = "LeaderboardViewModel"
    }

    init {
        loadGlobalLeaderboard()
        loadFriendsLeaderboard()
    }

    /**
     * Carica la classifica globale da tutti gli utenti
     */
    fun loadGlobalLeaderboard() {
        viewModelScope.launch {
            _isLoadingGlobal.value = true
            try {
                Log.d(TAG, "Caricando classifica globale...")

                val snapshot = firestore.collection("users")
                    .limit(500)
                    .get()
                    .await()

                val users = mutableListOf<LeaderboardUser>()

                // Carica tutti gli utenti con i loro dati reali
                for (doc in snapshot.documents) {
                    val userData = doc.data
                    if (userData != null) {
                        // MODIFICA: Carica i dati reali dai punti come fai per gli amici
                        val (realPoints, realRefuges) = getUserStatsFromPoints(doc.id)

                        val nome = userData["nome"] as? String ?: ""
                        val cognome = userData["cognome"] as? String ?: ""
                        val nickname = userData["nickname"] as? String ?: ""
                        val profileImageUrl = userData["profileImageUrl"] as? String

                        val displayName = if (nickname.isNotBlank()) {
                            nickname
                        } else {
                            "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"
                        }

                        val user = LeaderboardUser(
                            id = doc.id,
                            name = displayName,
                            points = realPoints,  // USA I DATI REALI
                            refugesCount = realRefuges,  // USA I DATI REALI
                            position = 0, // Verrà aggiornato dopo l'ordinamento
                            avatarResource = R.drawable.avatar_mario,
                            profileImageUrl = profileImageUrl
                        )

                        users.add(user)
                    }
                }

                // Ordina per punti e assegna le posizioni
                val sortedUsers = users.sortedByDescending { it.points }
                val usersWithPositions = sortedUsers.mapIndexed { index, user ->
                    user.copy(position = index + 1)
                }

                originalGlobalList = usersWithPositions
                _globalLeaderboard.value = usersWithPositions
                Log.d(TAG, "Classifica globale caricata: ${usersWithPositions.size} utenti")

            } catch (e: Exception) {
                Log.e(TAG, "Errore nel caricamento classifica globale: ${e.message}")
                _error.value = "Errore nel caricamento della classifica: ${e.message}"
            } finally {
                _isLoadingGlobal.value = false
            }
        }
    }

    private suspend fun getUserStatsFromPoints(userId: String): Pair<Int, Int> {
        return try {
            val userStatsDoc = firestore.collection("user_points_stats")
                .document(userId)
                .get()
                .await()

            val userStats = userStatsDoc.toObject(com.example.mountainpassport_girarifugi.data.model.UserPointsStats::class.java)

            if (userStats != null) {
                Pair(userStats.totalPoints, userStats.totalVisits)
            } else {
                Pair(0, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user stats for $userId", e)
            Pair(0, 0)
        }
    }

    /**
     * Carica la classifica degli amici dell'utente corrente
     */
    fun loadFriendsLeaderboard() {
        viewModelScope.launch {
            _isLoadingFriends.value = true
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    Log.w(TAG, "Utente non autenticato")
                    _friendsLeaderboard.value = emptyList()
                    originalFriendsList = emptyList()
                    return@launch
                }

                Log.d(TAG, "Caricando classifica amici per utente: $currentUserId")

                // Carica prima l'utente corrente per debug
                val currentUserDoc = firestore.collection("users")
                    .document(currentUserId)
                    .get()
                    .await()

                Log.d(TAG, "Documento utente corrente exists: ${currentUserDoc.exists()}")
                if (currentUserDoc.exists()) {
                    val data = currentUserDoc.data
                    Log.d(TAG, "Dati utente corrente: $data")
                }

                // 1. Carica la lista degli amici
                val friendsSnapshot = firestore.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .get()
                    .await()

                Log.d(TAG, "Trovati ${friendsSnapshot.documents.size} amici")

                val friendsUsers = mutableListOf<LeaderboardUser>()

                // Aggiungi l'utente corrente
                if (currentUserDoc.exists()) {
                    val currentUserData = currentUserDoc.data!!
                    val currentUser = createLeaderboardUser(currentUserId, currentUserData, 0)
                    Log.d(TAG, "Utente corrente: ${currentUser.name}, Punti: ${currentUser.points}")
                    friendsUsers.add(currentUser)
                }

                // Carica i dati di ogni amico
                val friendIds = friendsSnapshot.documents.map { it.id }
                Log.d(TAG, "ID amici: $friendIds")

                for (friendId in friendIds) {
                    try {
                        val friendDoc = firestore.collection("users")
                            .document(friendId)
                            .get()
                            .await()

                        if (friendDoc.exists()) {
                            val friendData = friendDoc.data!!
                            val friendUser = createLeaderboardUser(friendId, friendData, 0)
                            Log.d(TAG, "Amico caricato: ${friendUser.name}, Punti: ${friendUser.points}")
                            friendsUsers.add(friendUser)
                        } else {
                            Log.w(TAG, "Documento amico $friendId non esiste")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore nel caricare amico $friendId: ${e.message}")
                    }
                }

                // Ordina per punti e assegna le posizioni
                val sortedFriends = friendsUsers.sortedByDescending { it.points }
                val friendsWithPositions = sortedFriends.mapIndexed { index, user ->
                    user.copy(position = index + 1)
                }

                Log.d(TAG, "Lista finale ordinata:")
                friendsWithPositions.forEach { user ->
                    Log.d(TAG, "  ${user.position}. ${user.name}: ${user.points} punti")
                }

                originalFriendsList = friendsWithPositions
                _friendsLeaderboard.value = friendsWithPositions
                Log.d(TAG, "Classifica amici caricata: ${friendsWithPositions.size} utenti")

            } catch (e: Exception) {
                Log.e(TAG, "Errore nel caricamento classifica amici: ${e.message}")
                e.printStackTrace()
                _error.value = "Errore nel caricamento della classifica amici: ${e.message}"
            } finally {
                _isLoadingFriends.value = false
            }
        }
    }

    /**
     * Helper per creare un LeaderboardUser da dati Firebase
     */
    private fun createLeaderboardUser(
        userId: String,
        userData: Map<String, Any>,
        position: Int
    ): LeaderboardUser {
        val nome = userData["nome"] as? String ?: ""
        val cognome = userData["cognome"] as? String ?: ""
        val nickname = userData["nickname"] as? String ?: ""
        val points = (userData["points"] as? Long)?.toInt() ?: 0
        val refugesCount = (userData["refugesCount"] as? Long)?.toInt() ?: 0
        val profileImageUrl = userData["profileImageUrl"] as? String

        val displayName = if (nickname.isNotBlank()) {
            nickname
        } else {
            "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"
        }

        return LeaderboardUser(
            id = userId,
            name = displayName,
            points = points,
            refugesCount = refugesCount,
            position = position,
            avatarResource = R.drawable.avatar_mario,
            profileImageUrl = profileImageUrl
        )
    }

    /**
     * Funzioni di ricerca
     */
    fun searchInFriends(query: String) {
        if (query.isBlank()) {
            _friendsLeaderboard.value = originalFriendsList
        } else {
            val filteredFriends = originalFriendsList.filter { user ->
                user.name.contains(query, ignoreCase = true)
            }
            _friendsLeaderboard.value = filteredFriends
        }
    }

    fun searchInGlobal(query: String) {
        if (query.isBlank()) {
            _globalLeaderboard.value = originalGlobalList
        } else {
            val filteredUsers = originalGlobalList.filter { user ->
                user.name.contains(query, ignoreCase = true)
            }
            _globalLeaderboard.value = filteredUsers
        }
    }

    fun searchInGroups(query: String) {
        if (query.isBlank()) {
            _groupsLeaderboard.value = originalGroupsList
        } else {
            val filteredGroups = originalGroupsList.filter { user ->
                user.name.contains(query, ignoreCase = true)
            }
            _groupsLeaderboard.value = filteredGroups
        }
    }

    fun clearSearch() {
        _friendsLeaderboard.value = originalFriendsList
        _globalLeaderboard.value = originalGlobalList
        _groupsLeaderboard.value = originalGroupsList
    }

    /**
     * Aggiorna solo la classifica degli amici
     */
    fun refreshFriendsLeaderboard() {
        loadFriendsLeaderboard()
    }

    /**
     * Aggiorna solo la classifica globale
     */
    fun refreshGlobalLeaderboard() {
        loadGlobalLeaderboard()
    }

    fun clearError() {
        _error.value = null
    }
}