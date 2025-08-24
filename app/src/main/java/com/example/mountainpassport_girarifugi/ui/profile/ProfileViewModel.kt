package com.example.mountainpassport_girarifugi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

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

    init {
        loadUserData()
        loadStamps()
    }

    private fun loadUserData() {
        // Qui dovresti caricare i dati reali dell'utente da Firebase/Database
        // Per ora uso dati di esempio
        val currentUser = firebaseAuth.currentUser
        _currentUser.value = currentUser

        // Esempio di caricamento dati del profilo
        val profileData = ProfileData(
            fullName = currentUser?.displayName ?: "Marco Rossi",
            username = "marcorossi_explorer",
            monthlyScore = "1,245",
            visitedRefuges = "23"
        )
        _profileData.value = profileData
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

    // Metodi per caricare dati dal database (da implementare)
    fun loadUserProfileFromDatabase(userId: String) {
        // TODO: Implementa il caricamento dei dati del profilo dal database
    }

    fun loadStampsFromDatabase(userId: String) {
        // TODO: Implementa il caricamento dei timbri dal database
    }
}