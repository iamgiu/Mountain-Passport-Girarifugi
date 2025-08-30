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

// Data class per gli utenti che possono essere aggiunti come amici
data class AddFriendUser(
    val id: String,
    val name: String,
    val username: String,
    val points: Int,
    val refugesCount: Int,
    val avatarResource: Int,
    val isAlreadyFriend: Boolean = false,
    val isRequestSent: Boolean = false,
    val profileImageUrl: String? = null // AGGIUNTO per l'immagine profilo reale
)

class AddFriendsViewModel : ViewModel() {

    // LiveData per i risultati della ricerca
    private val _searchResults = MutableLiveData<List<AddFriendUser>>()
    val searchResults: LiveData<List<AddFriendUser>> = _searchResults

    // LiveData per gli stati di caricamento
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per gestire gli errori
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Dati mock per gruppi
    private val allGroups = listOf(
        AddFriendUser("g1", "Escursionisti Lombardia", "esc_lombardia", 156789, 89, R.drawable.avatar_mario),
        AddFriendUser("g2", "Amici della Montagna", "amici_montagna", 134567, 76, R.drawable.avatar_marco),
        AddFriendUser("g3", "Trekking Piemonte", "trek_piemonte", 112345, 65, R.drawable.avatar_luca),
        AddFriendUser("g4", "Alpinisti Uniti", "alpinisti_uniti", 98765, 54, R.drawable.avatar_giovanni),
        AddFriendUser("g5", "Rifugi & Sentieri", "rifugi_sentieri", 87654, 43, R.drawable.avatar_lucia),
        AddFriendUser("g6", "Camminate nel Verde", "camminate_verde", 76543, 38, R.drawable.avatar_sara),
        AddFriendUser("g7", "Outdoor Adventurers", "outdoor_adventurers", 65432, 32, R.drawable.avatar_mario),
        AddFriendUser("g8", "Mountain Lovers", "mountain_lovers", 54321, 28, R.drawable.avatar_marco),
        AddFriendUser("g9", "Vette e Valli", "vette_valli", 43210, 24, R.drawable.avatar_luca),
        AddFriendUser("g10", "Escursioni Domenicali", "esc_domenicali", 32109, 20, R.drawable.avatar_giovanni)
    )

    // Gestione gruppi
    private val joinedGroupRequests = mutableSetOf<String>()

    private val friendRepository = FriendRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        // Carica alcuni utenti di default
        loadDefaultUsers()
    }

    fun searchGroups(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val filteredGroups = if (query.isBlank()) {
                    allGroups.take(10) // Mostra solo i primi 10 se non c'è ricerca
                } else {
                    allGroups.filter { group ->
                        group.name.contains(query, ignoreCase = true)
                    }
                }

                // Aggiorna lo stato delle richieste
                val updatedGroups = filteredGroups.map { group ->
                    group.copy(isRequestSent = joinedGroupRequests.contains(group.id))
                }

                _searchResults.value = updatedGroups
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nella ricerca gruppi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDefaultUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Carica utenti da Firebase
                searchUsersFromFirebase("")
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento utenti: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadDefaultGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val defaultGroups = allGroups.take(10).map { group ->
                    group.copy(isRequestSent = joinedGroupRequests.contains(group.id))
                }
                _searchResults.value = defaultGroups
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento gruppi: ${e.message}"
            } finally {
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
                        // Aggiorna lista amici
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

    // NUOVA FUNZIONE: Controlla lo stato delle amicizie e richieste
    private suspend fun checkFriendshipStatus(userId: String, currentUserId: String): Pair<Boolean, Boolean> {
        return try {
            // Controlla se sono già amici
            val friendDoc = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(userId)
                .get()
                .await()

            val isAlreadyFriend = friendDoc.exists()

            // Controlla se c'è una richiesta pendente
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
                                val user = doc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)

                                if (doc.id != currentUserId &&
                                    (query.isBlank() ||
                                            user.nome.contains(query, ignoreCase = true) ||
                                            user.cognome.contains(query, ignoreCase = true) ||
                                            user.nickname.contains(query, ignoreCase = true))) {

                                    val (isAlreadyFriend, isRequestSent) = checkFriendshipStatus(doc.id, currentUserId)

                                    // OTTIENI LE STATISTICHE REALI DAL POINTSREPOSITORY
                                    val userStats = getUserStatsFromPoints(doc.id)

                                    val addFriendUser = AddFriendUser(
                                        id = doc.id,
                                        name = "${user.nome} ${user.cognome}".trim(),
                                        username = user.nickname,
                                        points = userStats.first, // PUNTI REALI
                                        refugesCount = userStats.second, // RIFUGI REALI
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

    // AGGIUNGI QUESTO METODO
    private suspend fun getUserStatsFromPoints(userId: String): Pair<Int, Int> {
        return try {
            // Ottieni le statistiche dal sistema punti
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
            android.util.Log.e("AddFriendsVM", "Error getting user stats", e)
            Pair(0, 0)
        }
    }

    fun joinGroup(group: AddFriendUser) {
        viewModelScope.launch {
            try {
                // Simula una chiamata API
                joinedGroupRequests.add(group.id)

                // Aggiorna la lista corrente marcando il gruppo come "richiesta inviata"
                val currentList = _searchResults.value ?: emptyList()
                val updatedList = currentList.map { currentGroup ->
                    if (currentGroup.id == group.id) {
                        currentGroup.copy(isRequestSent = true)
                    } else {
                        currentGroup
                    }
                }
                _searchResults.value = updatedList

            } catch (e: Exception) {
                _error.value = "Errore nell'invio della richiesta di accesso al gruppo: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}