package com.example.mountainpassport_girarifugi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mountainpassport_girarifugi.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

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
    val region: String
)

class ProfileViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // LiveData per i dati del profilo
    private val _profileData = MutableLiveData<ProfileData>()
    val profileData: LiveData<ProfileData> = _profileData

    // LiveData per la lista dei timbri
    private val _stamps = MutableLiveData<List<Stamp>>()
    val stamps: LiveData<List<Stamp>> = _stamps

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

    init {
        loadUserData()
        loadStamps()
    }

    private fun loadUserData() {
        val currentUser = firebaseAuth.currentUser
        _currentUser.value = currentUser

        if (currentUser != null) {
            loadUserProfileFromDatabase(currentUser.uid)
        } else {
            // Utente non autenticato, usa valori di default
            val profileData = ProfileData(
                fullName = "Utente",
                username = "utente_guest",
                monthlyScore = "0",
                visitedRefuges = "0"
            )
            _profileData.value = profileData
        }
    }

    private fun loadStamps() {
        // Qui dovresti caricare i timbri reali dal database
        // Per ora uso dati di esempio
        val sampleStamps = listOf(
            Stamp("Rifugio Città di Milano", "15/05/2024", "2581m", "Lombardia"),
            Stamp("Rifugio Branca", "22/05/2024", "2486m", "Lombardia"),
            Stamp("Rifugio Carate Brianza", "28/05/2024", "2636m", "Lombardia"),
            Stamp("Rifugio Pizzini", "05/06/2024", "2706m", "Lombardia"),
            Stamp("Rifugio Belviso", "12/06/2024", "2234m", "Lombardia"),
            Stamp("Rifugio Bonetta", "19/06/2024", "2458m", "Piemonte"),
            Stamp("Rifugio Schiena d'Asino", "26/06/2024", "2445m", "Piemonte"),
            Stamp("Rifugio Bertacchi", "03/07/2024", "2175m", "Lombardia")
        )
        _stamps.value = sampleStamps
    }

    fun refreshData() {
        loadUserData()
        loadStamps()
    }

    fun performLogout() {
        firebaseAuth.signOut()
        _logoutEvent.value = true
    }

    // Metodo per resettare l'evento di logout dopo averlo gestito
    fun onLogoutEventHandled() {
        _logoutEvent.value = false
    }

    // Implementazione del caricamento dei dati del profilo dal database
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
                                monthlyScore = "1,245", // TODO: Calcolare il punteggio reale
                                visitedRefuges = "23"    // TODO: Contare i rifugi visitati reali
                            )
                            _profileData.value = profileData
                        } else {
                            // Documento esiste ma non è un oggetto User valido
                            _loadingError.value = "Errore nel caricamento del profilo utente"
                            setDefaultProfileData()
                        }
                    } catch (e: Exception) {
                        _loadingError.value = "Errore nella conversione dei dati: ${e.message}"
                        setDefaultProfileData()
                    }
                } else {
                    // Il documento non esiste, l'utente non ha ancora completato il profilo
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

    fun loadStampsFromDatabase(userId: String) {
        // TODO: Implementa il caricamento dei timbri dal database
        // Per ora manteniamo i dati di esempio
        loadStamps()
    }

    // Metodo per ricaricare i dati utente (utile dopo aver aggiornato il profilo)
    fun reloadUserProfile() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            loadUserProfileFromDatabase(currentUser.uid)
        }
    }
}