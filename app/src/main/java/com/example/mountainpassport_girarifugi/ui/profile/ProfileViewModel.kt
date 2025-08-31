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
import com.example.mountainpassport_girarifugi.utils.UserManager
import com.google.firebase.firestore.ListenerRegistration
import com.example.mountainpassport_girarifugi.ui.profile.FriendRequest
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

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

    // LiveData per i dati del profilo
    private val _profileData = MutableLiveData<ProfileData>()
    val profileData: LiveData<ProfileData> = _profileData

    // LiveData per la lista dei timbri
    private val _stamps = MutableLiveData<List<Stamp>>()
    val stamps: LiveData<List<Stamp>> = _stamps

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
        loadUserData()
        loadStamps()
        loadGroups()
        loadFriends()
        startListeningForFriendRequests()

        observePointsUpdates()
    }

    /**
     * NUOVO: Osserva gli aggiornamenti dei punti da altri Fragment
     */
    private fun observePointsUpdates() {
        // Observer per quando vengono guadagnati punti
        com.example.mountainpassport_girarifugi.ui.map.RifugioSavedEventBus.pointsUpdatedEvent.observeForever { pointsEarned ->
            // Ricarica i dati del profilo quando vengono guadagnati punti
            loadUserData()
            loadStamps()
        }

        // Observer per quando le statistiche utente vengono aggiornate
        com.example.mountainpassport_girarifugi.ui.map.RifugioSavedEventBus.userStatsUpdatedEvent.observeForever {
            // Ricarica i dati del profilo
            loadUserData()
            loadStamps()
        }
    }

    private fun loadUserData() {
        val currentUser = firebaseAuth.currentUser
        _currentUser.value = currentUser

        if (currentUser != null) {
            loadUserProfileFromDatabase(currentUser.uid)
            loadUserPointsStats(currentUser.uid) // ASSICURATI che questo venga chiamato
        } else {
            val profileData = ProfileData(
                fullName = "Utente",
                username = "utente_guest",
                monthlyScore = "0",
                visitedRefuges = "0"
            )
            _profileData.value = profileData
        }
    }

    /**
     * MODIFICATO: Carica le statistiche dei punti dell'utente
     */
    private fun loadUserPointsStats(userId: String) {
        viewModelScope.launch {
            try {
                val stats = withContext(Dispatchers.IO) {
                    pointsRepository.getUserPointsStats(userId)
                }

                // NUOVO: Se non ci sono statistiche, prova a caricarle dal documento users
                val finalStats = stats ?: loadStatsFromUserDocument(userId)

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

                    android.util.Log.d("ProfileViewModel", "Statistiche aggiornate: ${finalStats.totalPoints} punti, ${finalStats.totalVisits} visite")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Errore nel caricare le statistiche: ${e.message}")
            }
        }
    }

    /**
     * NUOVO: Carica statistiche dal documento users se user_points_stats è vuoto
     */
    private suspend fun loadStatsFromUserDocument(userId: String): com.example.mountainpassport_girarifugi.data.model.UserPointsStats? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val points = userDoc.getLong("points")?.toInt() ?: 0
                val refugesCount = userDoc.getLong("refugesCount")?.toInt() ?: 0

                // Crea un oggetto UserPointsStats di base
                com.example.mountainpassport_girarifugi.data.model.UserPointsStats(
                    userId = userId,
                    totalPoints = points,
                    totalVisits = refugesCount,
                    monthlyPoints = 0, // Non disponibile nel documento users
                    monthlyVisits = 0  // Non disponibile nel documento users
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileViewModel", "Errore nel caricare stats da users: ${e.message}")
            null
        }
    }

    /**
     * MODIFICATO: Ricarica tutti i dati
     */
    fun refreshData() {
        android.util.Log.d("ProfileViewModel", "Refreshing profile data...")
        loadUserData()
        loadStamps()
        loadGroups()
        loadFriends()

        // NUOVO: Forza il ricaricamento delle statistiche
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            loadUserPointsStats(currentUser.uid)
        }
    }

    private fun loadStamps() {
        val currentUserId = UserManager.getCurrentUserId()
        if (currentUserId != null) {
            loadUserVisits(currentUserId)
        } else {
            // Se non c'è utente autenticato, mostra lista vuota
            _stamps.value = emptyList()
        }
    }

    /**
     * Carica le visite dell'utente e le converte in timbri - VERSIONE CORRETTA
     */
    private fun loadUserVisits(userId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProfileViewModel", "Caricando visite per userId: $userId")

                val visits = withContext(Dispatchers.IO) {
                    pointsRepository.getUserVisits(userId, 200)
                }

                android.util.Log.d("ProfileViewModel", "Visite caricate: ${visits.size}")

                if (visits.isEmpty()) {
                    android.util.Log.w("ProfileViewModel", "Nessuna visita trovata")
                    _stamps.value = emptyList()
                    return@launch
                }

                val rifugioRepo = RifugioRepository(context)

                // Raggruppa per rifugioId e prendi solo la prima visita
                val stampsMap = mutableMapOf<Int, Stamp>()

                for (visit in visits) {
                    // Se questo rifugio non è già nei timbri, aggiungilo
                    if (!stampsMap.containsKey(visit.rifugioId)) {
                        val rifugio = withContext(Dispatchers.IO) {
                            rifugioRepo.getRifugioById(visit.rifugioId)
                        }

                        val stamp = Stamp(
                            refugeName = rifugio?.nome ?: "Rifugio #${visit.rifugioId}",
                            date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                .format(visit.visitDate.toDate()),
                            altitude = rifugio?.altitudine?.toString()?.plus(" m") ?: "${visit.pointsEarned} punti",
                            region = rifugio?.regione ?: rifugio?.localita ?: "Visitato",
                            imageResId = R.drawable.stamps
                        )

                        stampsMap[visit.rifugioId] = stamp
                        android.util.Log.d("ProfileViewModel", "Timbro aggiunto: ${stamp.refugeName}")
                    }
                }

                // Ordina per data di visita (più recenti per primi) usando la data originale
                val sortedStamps = stampsMap.values.sortedByDescending { stamp ->
                    // Trova la visita corrispondente per ordinare per data originale
                    visits.find { visit ->
                        val rifugio = runBlocking { rifugioRepo.getRifugioById(visit.rifugioId) }
                        rifugio?.nome == stamp.refugeName || "Rifugio #${visit.rifugioId}" == stamp.refugeName
                    }?.visitDate?.seconds ?: 0
                }

                _stamps.value = sortedStamps
                android.util.Log.d("ProfileViewModel", "Timbri finali: ${sortedStamps.size}")

            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Errore nel caricare le visite: ${e.message}", e)
                _stamps.value = emptyList()
            }
        }
    }

    private fun loadGroups() {
        // Dati di esempio per i gruppi
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
            ),
            Group(
                id = "group3",
                name = "Trekkers Milano",
                memberCount = 25,
                description = "Gruppo di trekking della zona di Milano",
            ),
            Group(
                id = "group4",
                name = "Escursioni Weekend",
                memberCount = 15,
                description = "Escursioni nei weekend",
            )
        )
        _groups.value = sampleGroups
    }

    // Implementazione del caricamento dei dati del profilo dal database
    fun performLogout() {
        firebaseAuth.signOut()
        _logoutEvent.value = true
    }

    fun onLogoutEventHandled() {
        _logoutEvent.value = false
    }

    fun loadUserProfileFromDatabase(userId: String) {
        _isLoading.value = true

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                _isLoading.value = false

                if (document.exists()) {
                    try {
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            val fullName = "${user.nome} ${user.cognome}".trim()
                            val displayFullName = if (fullName.isBlank()) "Nome non disponibile" else fullName
                            val displayNickname = if (user.nickname.isBlank()) "Nickname non disponibile" else user.nickname

                            val profileData = ProfileData(
                                fullName = displayFullName,
                                username = displayNickname,
                                monthlyScore = "0", // Verrà aggiornato da loadUserPointsStats
                                visitedRefuges = "0"  // Verrà aggiornato da loadUserPointsStats
                            )
                            _profileData.value = profileData
                        } else {
                            _loadingError.value = "Errore nel caricamento del profilo utente"
                            setDefaultProfileData()
                        }
                    } catch (e: Exception) {
                        _loadingError.value = "Errore nella conversione dei dati: ${e.message}"
                        setDefaultProfileData()
                    }
                } else {
                    _loadingError.value = "Profilo utente non trovato. Completa la registrazione."
                    setDefaultProfileData()
                }
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _loadingError.value = "Errore di connessione: ${exception.message}"
                setDefaultProfileData()
            }
    }

    private fun setDefaultProfileData() {
        val currentUser = firebaseAuth.currentUser
        val profileData = ProfileData(
            fullName = currentUser?.displayName ?: "Nome non disponibile",
            username = currentUser?.email?.substringBefore("@") ?: "Utente",
            monthlyScore = "0",
            visitedRefuges = "0"
        )
        _profileData.value = profileData
    }

    fun reloadUserProfile() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            loadUserProfileFromDatabase(currentUser.uid)
        }
    }

    fun startListeningForFriendRequests() {
        friendRequestsListener = friendRepository.listenForFriendRequests { requests ->
            _friendRequests.value = requests
        }
    }

    fun stopListeningForFriendRequests() {
        friendRequestsListener?.remove()
        friendRequestsListener = null
    }

    fun acceptFriendRequest(requestId: String) {
        friendRepository.acceptFriendRequest(requestId) { success, error ->
            if (!success) {
                _loadingError.value = error ?: "Errore nell'accettare la richiesta"
            }
            // Refresh friends list
            loadFriends()
        }
    }

    private fun loadFriends() {
        val currentUser = firebaseAuth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .collection("friends")
            .get()
            .addOnSuccessListener { snapshot ->
                val friendsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friend::class.java)
                }
                _friends.value = friendsList
            }
            .addOnFailureListener { e ->
                _loadingError.value = "Errore nel caricamento amici: ${e.message}"
            }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForFriendRequests()
    }

    // Metodo per caricare i gruppi dal database (da implementare)
    fun loadGroupsFromDatabase(userId: String) {
        // TODO: Implementa il caricamento dei gruppi dal database
        // Per ora manteniamo i dati di esempio
        loadGroups()
    }
}