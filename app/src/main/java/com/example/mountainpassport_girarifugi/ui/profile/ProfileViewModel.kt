package com.example.mountainpassport_girarifugi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mountainpassport_girarifugi.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mountainpassport_girarifugi.data.repository.FriendRepository
import com.example.mountainpassport_girarifugi.data.repository.PointsRepository
import com.google.firebase.firestore.ListenerRegistration
import com.example.mountainpassport_girarifugi.ui.profile.FriendRequest
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.model.UserPoints
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import android.util.Log

// Data class per rappresentare i dati del profilo utente
data class ProfileData(
    val fullName: String,
    val username: String,
    val monthlyScore: String,
    val visitedRefuges: String
)

// Data class per rappresentare un timbro
data class Stamp(
    val refugeName: String,
    val date: String,
    val altitude: String,
    val region: String,
    val imageResId: Int = R.drawable.stamps
)

// Data class per rappresentare un gruppo
data class Group(
    val id: String,
    val name: String,
    val memberCount: Int,
    val description: String = "",
)

class ProfileViewModel(private val context: Context) : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val friendRepository = FriendRepository()
    private val pointsRepository = PointsRepository(context)
    private var friendRequestsListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    // Flag per evitare caricamenti multipli
    private var isLoadingData = false
    private var isStatsLoaded = false

    // LiveData per i dati del profilo
    private val _profileData = MutableLiveData<ProfileData>()
    val profileData: LiveData<ProfileData> = _profileData

    // LiveData per la lista dei timbri
    private val _stamps = MutableLiveData<List<UserPoints>>()
    val stamps: LiveData<List<UserPoints>> = _stamps

    // LiveData per la lista dei gruppi
    private val _groups = MutableLiveData<List<Group>>()
    val groups: LiveData<List<Group>> = _groups

    // LiveData per gestire il logout
    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    // LiveData per l'utente corrente
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    // LiveData per gestire errori di caricamento
    private val _loadingError = MutableLiveData<String>()
    val loadingError: LiveData<String> = _loadingError

    // LiveData per lo stato di caricamento
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per le richieste d'amicizia
    private val _friendRequests = MutableLiveData<List<FriendRequest>>()
    val friendRequests: LiveData<List<FriendRequest>> = _friendRequests

    // LiveData amici
    private val _friends = MutableLiveData<List<Friend>>()
    val friends: LiveData<List<Friend>> = _friends

    init {
        Log.d(TAG, "ProfileViewModel inizializzato")
        try {
            loadInitialData()
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'inizializzazione", e)
            _loadingError.value = "Errore nell'inizializzazione: ${e.message}"
        }
    }

    /**
     * Caricamento iniziale dei dati
     */
    private fun loadInitialData() {
        if (isLoadingData) {
            Log.d(TAG, "Caricamento già in corso, saltato")
            return
        }

        isLoadingData = true
        _isLoading.value = true

        try {
            loadUserData()
            loadStamps()
            loadGroups()
            loadFriends()
            startListeningForFriendRequests()
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricamento iniziale", e)
            _loadingError.value = "Errore nel caricamento: ${e.message}"
        } finally {
            isLoadingData = false
            _isLoading.value = false
        }
    }

    private fun loadUserData() {
        try {
            val currentUser = firebaseAuth.currentUser
            _currentUser.value = currentUser

            if (currentUser != null) {
                Log.d(TAG, "Caricamento dati per utente: ${currentUser.uid}")
                loadUserProfileFromDatabase(currentUser.uid)

                // Carica le statistiche solo se non già caricate
                if (!isStatsLoaded) {
                    loadUserPointsStats(currentUser.uid)
                    isStatsLoaded = true
                }
            } else {
                Log.d(TAG, "Utente non autenticato, caricamento dati di default")
                setDefaultProfileData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore in loadUserData", e)
            _loadingError.value = "Errore nel caricamento utente: ${e.message}"
            setDefaultProfileData()
        }
    }

    /**
     * Carica le statistiche dei punti dell'utente - VERSIONE SICURA
     */
    private fun loadUserPointsStats(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Caricamento statistiche per: $userId")

                val stats = withContext(Dispatchers.IO) {
                    try {
                        pointsRepository.getUserPointsStats(userId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Errore nel caricamento stats da pointsRepository: ${e.message}")
                        null
                    }
                }

                // Se non ci sono statistiche, prova a caricarle dal documento users
                val finalStats = stats ?: withContext(Dispatchers.IO) {
                    loadStatsFromUserDocument(userId)
                }

                if (finalStats != null) {
                    // Aggiorna le statistiche nel profilo
                    val currentProfile = _profileData.value ?: ProfileData(
                        fullName = "Utente",
                        username = "utente_guest",
                        monthlyScore = "0",
                        visitedRefuges = "0"
                    )

                    val updatedProfile = currentProfile.copy(
                        monthlyScore = finalStats.monthlyPoints.toString(),
                        visitedRefuges = finalStats.totalVisits.toString()
                    )

                    _profileData.value = updatedProfile
                    Log.d(TAG, "Statistiche aggiornate: ${finalStats.totalPoints} punti, ${finalStats.totalVisits} visite")
                } else {
                    Log.w(TAG, "Nessuna statistica trovata per l'utente")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Errore nel caricare le statistiche: ${e.message}", e)
                // Non propagare l'errore, mantieni i dati esistenti
            }
        }
    }

    /**
     * Carica statistiche dal documento users - VERSIONE SICURA
     */
    private suspend fun loadStatsFromUserDocument(userId: String): com.example.mountainpassport_girarifugi.data.model.UserPointsStats? {
        return try {
            Log.d(TAG, "Caricamento stats da documento users per: $userId")

            val userDoc = firestore.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val points = userDoc.getLong("points")?.toInt() ?: 0
                val refugesCount = userDoc.getLong("refugesCount")?.toInt() ?: 0

                Log.d(TAG, "Stats da users: $points punti, $refugesCount rifugi")

                com.example.mountainpassport_girarifugi.data.model.UserPointsStats(
                    userId = userId,
                    totalPoints = points,
                    totalVisits = refugesCount,
                    monthlyPoints = 0,
                    monthlyVisits = 0
                )
            } else {
                Log.w(TAG, "Documento users non trovato per: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricare stats da users: ${e.message}", e)
            null
        }
    }

    /**
     * Ricarica tutti i dati - VERSIONE SICURA
     */
    fun refreshData() {
        Log.d(TAG, "Refresh dei dati del profilo...")

        try {
            // Reset del flag per permettere il ricaricamento delle stats
            isStatsLoaded = false

            loadUserData()
            loadStamps()
            loadGroups()
            loadFriends()

        } catch (e: Exception) {
            Log.e(TAG, "Errore nel refresh: ${e.message}", e)
            _loadingError.value = "Errore nel refresh: ${e.message}"
        }
    }

    private fun loadStamps() {
        try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                loadUserStampsFromFirestore(currentUser.uid)
            } else {
                _stamps.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore in loadStamps", e)
            _stamps.value = emptyList()
        }
    }

    private fun loadUserStampsFromFirestore(userId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("stamps")
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val stampsList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val refugeName = doc.getString("refugeName") ?: return@mapNotNull null
                                val dateMillis = doc.getLong("date") ?: 0L

                                UserPoints(
                                    rifugioId = 0,
                                    rifugioName = refugeName,
                                    visitDate = com.google.firebase.Timestamp(dateMillis / 1000, 0),
                                    pointsEarned = 0
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Errore nel processare stamp: ${e.message}")
                                null
                            }
                        }
                        _stamps.value = stampsList
                        Log.d(TAG, "Caricati ${stampsList.size} timbri")
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore nel processare timbri", e)
                        _stamps.value = emptyList()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore caricamento timbri: ${e.message}", e)
                    _stamps.value = emptyList()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Errore in loadUserStampsFromFirestore", e)
            _stamps.value = emptyList()
        }
    }

    private fun loadGroups() {
        try {
            val sampleGroups = listOf(
                Group(
                    id = "group1",
                    name = "Amanti delle Alpi",
                    memberCount = 12,
                    description = "Gruppo per esplorare le Alpi lombarde",
                ),
                Group(
                    id = "group2",
                    name = "Rifugi del Nord",
                    memberCount = 8,
                    description = "Scopriamo insieme i rifugi del nord Italia",
                )
            )
            _groups.value = sampleGroups
        } catch (e: Exception) {
            Log.e(TAG, "Errore in loadGroups", e)
            _groups.value = emptyList()
        }
    }

    fun performLogout() {
        try {
            firebaseAuth.signOut()
            _logoutEvent.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel logout", e)
            _loadingError.value = "Errore nel logout: ${e.message}"
        }
    }

    fun onLogoutEventHandled() {
        _logoutEvent.value = false
    }

    fun loadUserProfileFromDatabase(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                try {
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            val fullName = "${user.nome ?: ""} ${user.cognome ?: ""}".trim()
                            val displayFullName = if (fullName.isBlank()) "Nome non disponibile" else fullName
                            val displayNickname = if (user.nickname.isNullOrBlank()) "Nickname non disponibile" else user.nickname

                            val profileData = ProfileData(
                                fullName = displayFullName,
                                username = displayNickname,
                                monthlyScore = "0", // Verrà aggiornato da loadUserPointsStats
                                visitedRefuges = "0"  // Verrà aggiornato da loadUserPointsStats
                            )
                            _profileData.value = profileData
                            Log.d(TAG, "Profilo caricato: $displayFullName")
                        } else {
                            Log.w(TAG, "User object è null")
                            setDefaultProfileData()
                        }
                    } else {
                        Log.w(TAG, "Documento utente non trovato")
                        setDefaultProfileData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Errore nella conversione dei dati", e)
                    setDefaultProfileData()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Errore nel caricamento profilo", exception)
                _loadingError.value = "Errore di connessione: ${exception.message}"
                setDefaultProfileData()
            }
    }

    private fun setDefaultProfileData() {
        try {
            val currentUser = firebaseAuth.currentUser
            val profileData = ProfileData(
                fullName = currentUser?.displayName ?: "Nome non disponibile",
                username = currentUser?.email?.substringBefore("@") ?: "Utente",
                monthlyScore = "0",
                visitedRefuges = "0"
            )
            _profileData.value = profileData
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel settare dati di default", e)
        }
    }

    fun reloadUserProfile() {
        try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                loadUserProfileFromDatabase(currentUser.uid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel reload profilo", e)
            _loadingError.value = "Errore nel ricaricamento: ${e.message}"
        }
    }

    fun startListeningForFriendRequests() {
        try {
            if (friendRequestsListener != null) {
                Log.d(TAG, "Friend requests listener già attivo")
                return
            }

            friendRequestsListener = friendRepository.listenForFriendRequests { requests ->
                _friendRequests.value = requests
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'avviare friend requests listener", e)
        }
    }

    fun stopListeningForFriendRequests() {
        try {
            friendRequestsListener?.remove()
            friendRequestsListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel fermare friend requests listener", e)
        }
    }

    fun acceptFriendRequest(requestId: String) {
        try {
            friendRepository.acceptFriendRequest(requestId) { success, error ->
                if (!success) {
                    _loadingError.value = error ?: "Errore nell'accettare la richiesta"
                } else {
                    loadFriends() // Refresh friends list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'accettare richiesta", e)
            _loadingError.value = "Errore nell'accettare richiesta: ${e.message}"
        }
    }

    private fun loadFriends() {
        try {
            val currentUser = firebaseAuth.currentUser ?: return

            firestore.collection("users")
                .document(currentUser.uid)
                .collection("friends")
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val friendsList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Friend::class.java)
                            } catch (e: Exception) {
                                Log.w(TAG, "Errore nel processare amico: ${e.message}")
                                null
                            }
                        }
                        _friends.value = friendsList
                        Log.d(TAG, "Caricati ${friendsList.size} amici")
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore nel processare amici", e)
                        _friends.value = emptyList()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore nel caricamento amici: ${e.message}", e)
                    _loadingError.value = "Errore nel caricamento amici: ${e.message}"
                    _friends.value = emptyList()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Errore in loadFriends", e)
            _friends.value = emptyList()
        }
    }

    override fun onCleared() {
        Log.d(TAG, "ViewModel destroyed")
        try {
            stopListeningForFriendRequests()
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella pulizia ViewModel", e)
        }
        super.onCleared()
    }
}