package com.example.mountainpassport_girarifugi.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mountainpassport_girarifugi.R

// Data class per i dati degli utenti della leaderboard
data class LeaderboardUser(
    val name: String,
    val points: Int,
    val avatarResource: Int,
    val position: Int,
    val refugesCount: Int = 0
)

class LeaderboardViewModel : ViewModel() {

    // LiveData per i dati degli amici
    private val _friendsLeaderboard = MutableLiveData<List<LeaderboardUser>>()
    val friendsLeaderboard: LiveData<List<LeaderboardUser>> = _friendsLeaderboard

    // LiveData per i dati globali
    private val _globalLeaderboard = MutableLiveData<List<LeaderboardUser>>()
    val globalLeaderboard: LiveData<List<LeaderboardUser>> = _globalLeaderboard

    // LiveData per i dati dei gruppi
    private val _groupsLeaderboard = MutableLiveData<List<LeaderboardUser>>()
    val groupsLeaderboard: LiveData<List<LeaderboardUser>> = _groupsLeaderboard

    // LiveData per gestire gli stati di caricamento
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per gestire gli errori
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // Carica i dati iniziali
        loadFriendsLeaderboard()
        loadGlobalLeaderboard()
        loadGroupsLeaderboard()
    }

    fun loadFriendsLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Qui metteresti la chiamata al repository/API
                // Per ora uso dati mock
                val friends = getFriendsMockData()
                _friendsLeaderboard.value = friends
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento degli amici: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGlobalLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Chiamata API per dati globali
                val globalData = getGlobalMockData()
                _globalLeaderboard.value = globalData
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento della classifica globale: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGroupsLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Chiamata API per dati dei gruppi
                val groupsData = getGroupsMockData()
                _groupsLeaderboard.value = groupsData
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento dei gruppi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Funzione per ricaricare tutti i dati
    fun refreshAllData() {
        loadFriendsLeaderboard()
        loadGlobalLeaderboard()
        loadGroupsLeaderboard()
    }

    // Mock data - sostituisci con chiamate al repository
    private fun getFriendsMockData(): List<LeaderboardUser> {
        return listOf(
            LeaderboardUser("Mario", 34897, R.drawable.avatar_mario, 1, 25),
            LeaderboardUser("Marco", 25567, R.drawable.avatar_marco, 2, 20),
            LeaderboardUser("Luca", 19508, R.drawable.avatar_luca, 3, 18),
            LeaderboardUser("Giovanni", 8900, R.drawable.avatar_giovanni, 4, 15),
            LeaderboardUser("Lucia", 8900, R.drawable.avatar_lucia, 5, 12),
            LeaderboardUser("Sara", 8900, R.drawable.avatar_sara, 6, 10),
            LeaderboardUser("Anna", 7500, R.drawable.avatar_sara, 7, 8),
            LeaderboardUser("Paolo", 6200, R.drawable.avatar_sara, 8, 6),
            LeaderboardUser("Marta", 5800, R.drawable.avatar_sara, 9, 5),
            LeaderboardUser("Federico", 4900, R.drawable.avatar_sara, 10, 4)
        )
    }

    private fun getGlobalMockData(): List<LeaderboardUser> {
        return listOf(
            LeaderboardUser("Alex_Mountain", 89453, R.drawable.avatar_mario, 1, 67),
            LeaderboardUser("Peak_Hunter", 78234, R.drawable.avatar_marco, 2, 58),
            LeaderboardUser("Summit_King", 65421, R.drawable.avatar_luca, 3, 52),
            LeaderboardUser("Alpine_Pro", 54123, R.drawable.avatar_giovanni, 4, 45),
            LeaderboardUser("Trail_Master", 43876, R.drawable.avatar_lucia, 5, 38)
        )
    }

    private fun getGroupsMockData(): List<LeaderboardUser> {
        return listOf(
            LeaderboardUser("Team Alps", 156789, R.drawable.avatar_mario, 1, 89),
            LeaderboardUser("Mountain Crew", 134567, R.drawable.avatar_marco, 2, 76),
            LeaderboardUser("Peak Seekers", 112345, R.drawable.avatar_luca, 3, 65),
            LeaderboardUser("Summit Squad", 98765, R.drawable.avatar_giovanni, 4, 54),
            LeaderboardUser("Trail Blazers", 87654, R.drawable.avatar_lucia, 5, 43)
        )
    }

    // Funzioni di utilità per ordinamento
    fun sortByPoints(users: List<LeaderboardUser>): List<LeaderboardUser> {
        return users.sortedByDescending { it.points }
            .mapIndexed { index, user -> user.copy(position = index + 1) }
    }

    fun sortByRefuges(users: List<LeaderboardUser>): List<LeaderboardUser> {
        return users.sortedByDescending { it.refugesCount }
            .mapIndexed { index, user -> user.copy(position = index + 1) }
    }
}