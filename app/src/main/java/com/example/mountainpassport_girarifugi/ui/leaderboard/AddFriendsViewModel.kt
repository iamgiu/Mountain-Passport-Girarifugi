package com.example.mountainpassport_girarifugi.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mountainpassport_girarifugi.R

// Data class per gli utenti che possono essere aggiunti come amici
data class AddFriendUser(
    val id: String,
    val name: String,
    val points: Int,
    val refugesCount: Int,
    val avatarResource: Int,
    val isAlreadyFriend: Boolean = false,
    val isRequestSent: Boolean = false
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

    // Dati mock per utenti
    private val allUsers = listOf(
        AddFriendUser("1", "Alessandro Bianchi", 4200, 12, R.drawable.avatar_giovanni),
        AddFriendUser("2", "Francesca Verde", 3800, 8, R.drawable.avatar_lucia),
        AddFriendUser("3", "Matteo Rossi", 5200, 15, R.drawable.avatar_mario),
        AddFriendUser("4", "Sofia Neri", 6800, 22, R.drawable.avatar_sara),
        AddFriendUser("5", "Lorenzo Blu", 2900, 6, R.drawable.avatar_marco),
        AddFriendUser("6", "Giulia Gialli", 7200, 28, R.drawable.avatar_lucia),
        AddFriendUser("7", "Andrea Viola", 1800, 4, R.drawable.avatar_giovanni),
        AddFriendUser("8", "Chiara Rosa", 8400, 35, R.drawable.avatar_sara),
        AddFriendUser("9", "Davide Arancione", 3600, 11, R.drawable.avatar_marco),
        AddFriendUser("10", "Valentina Grigia", 5800, 18, R.drawable.avatar_lucia),
        AddFriendUser("11", "Simone Marrone", 4100, 13, R.drawable.avatar_giovanni),
        AddFriendUser("12", "Elena Azzurra", 6200, 20, R.drawable.avatar_sara),
        AddFriendUser("13", "Filippo Oro", 9100, 42, R.drawable.avatar_mario),
        AddFriendUser("14", "Martina Argento", 7800, 31, R.drawable.avatar_lucia),
        AddFriendUser("15", "Riccardo Bronzo", 2400, 5, R.drawable.avatar_marco)
    )

    // Dati mock per gruppi
    private val allGroups = listOf(
        AddFriendUser("g1", "Escursionisti Lombardia", 156789, 89, R.drawable.avatar_mario),
        AddFriendUser("g2", "Amici della Montagna", 134567, 76, R.drawable.avatar_marco),
        AddFriendUser("g3", "Trekking Piemonte", 112345, 65, R.drawable.avatar_luca),
        AddFriendUser("g4", "Alpinisti Uniti", 98765, 54, R.drawable.avatar_giovanni),
        AddFriendUser("g5", "Rifugi & Sentieri", 87654, 43, R.drawable.avatar_lucia),
        AddFriendUser("g6", "Camminate nel Verde", 76543, 38, R.drawable.avatar_sara),
        AddFriendUser("g7", "Outdoor Adventurers", 65432, 32, R.drawable.avatar_mario),
        AddFriendUser("g8", "Mountain Lovers", 54321, 28, R.drawable.avatar_marco),
        AddFriendUser("g9", "Vette e Valli", 43210, 24, R.drawable.avatar_luca),
        AddFriendUser("g10", "Escursioni Domenicali", 32109, 20, R.drawable.avatar_giovanni)
    )

    // Set per tracciare le richieste inviate
    private val sentFriendRequests = mutableSetOf<String>()
    private val joinedGroupRequests = mutableSetOf<String>()

    init {
        // Carica alcuni utenti di default
        loadDefaultUsers()
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val filteredUsers = if (query.isBlank()) {
                    allUsers.take(10) // Mostra solo i primi 10 se non c'è ricerca
                } else {
                    allUsers.filter { user ->
                        user.name.contains(query, ignoreCase = true)
                    }
                }

                // Aggiorna lo stato delle richieste
                val updatedUsers = filteredUsers.map { user ->
                    user.copy(isRequestSent = sentFriendRequests.contains(user.id))
                }

                _searchResults.value = updatedUsers
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nella ricerca utenti: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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
                val defaultUsers = allUsers.take(10).map { user ->
                    user.copy(isRequestSent = sentFriendRequests.contains(user.id))
                }
                _searchResults.value = defaultUsers
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento utenti: ${e.message}"
            } finally {
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
            try {
                // Simula una chiamata API
                sentFriendRequests.add(user.id)

                // Aggiorna la lista corrente marcando l'utente come "richiesta inviata"
                val currentList = _searchResults.value ?: emptyList()
                val updatedList = currentList.map { currentUser ->
                    if (currentUser.id == user.id) {
                        currentUser.copy(isRequestSent = true)
                    } else {
                        currentUser
                    }
                }
                _searchResults.value = updatedList

            } catch (e: Exception) {
                _error.value = "Errore nell'invio della richiesta di amicizia: ${e.message}"
            }
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