package com.example.mountainpassport_girarifugi.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.Review
import com.example.mountainpassport_girarifugi.data.model.RifugioStats
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio
import com.example.mountainpassport_girarifugi.data.model.RifugioPoints
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import com.example.mountainpassport_girarifugi.data.repository.PointsRepository
import com.example.mountainpassport_girarifugi.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object RifugioSavedEventBus {
    private val _rifugioSavedEvent = MutableLiveData<Unit>()
    val rifugioSavedEvent: LiveData<Unit> = _rifugioSavedEvent

    private val _pointsUpdatedEvent = MutableLiveData<Int>()
    val pointsUpdatedEvent: LiveData<Int> = _pointsUpdatedEvent

    private val _userStatsUpdatedEvent = MutableLiveData<Unit>()
    val userStatsUpdatedEvent: LiveData<Unit> = _userStatsUpdatedEvent

    fun notifyRifugioSaved() {
        _rifugioSavedEvent.value = Unit
    }

    fun notifyPointsUpdated(pointsEarned: Int) {
        _pointsUpdatedEvent.value = pointsEarned
    }

    fun notifyUserStatsUpdated() {
        _userStatsUpdatedEvent.value = Unit
    }
}

class CabinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RifugioRepository(application)
    private val pointsRepository = PointsRepository(application)

    private val _rifugio = MutableLiveData<Rifugio>()
    val rifugio: LiveData<Rifugio> = _rifugio

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

    private val _reviews = MutableLiveData<List<Review>>()
    val reviews: LiveData<List<Review>> = _reviews

    private val _stats = MutableLiveData<RifugioStats?>()
    val stats: LiveData<RifugioStats?> = _stats

    private val _userPoints = MutableLiveData<Int>()
    val userPoints: LiveData<Int> = _userPoints

    /**
     * Carica i dati del rifugio dall'ID
     */
    fun loadRifugio(rifugioId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val rifugioData = repository.getRifugioById(rifugioId)

                if (rifugioData != null) {
                    _rifugio.value = rifugioData
                    // Carica anche i dati dinamici
                    loadDynamicData(rifugioId)
                } else {
                    _error.value = "Rifugio non trovato"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Errore caricamento rifugio: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Carica i dati dinamici (recensioni, statistiche, stato salvato)
     */
    private suspend fun loadDynamicData(rifugioId: Int) {
        try {
            android.util.Log.d("CabinViewModel", "Carico i dati dinamici per il seguente rifugio: $rifugioId")

            val reviews = repository.getReviewsForRifugio(rifugioId)
            _reviews.value = reviews

            val stats = repository.getRifugioStats(rifugioId)
            _stats.value = stats

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
            val doc = FirebaseFirestore.getInstance()
                .collection("saved_rifugi")
                .document(userId)
                .get()
                .await()

            val savedIds = doc.get("rifugi") as? List<Long> ?: emptyList()
            _isSaved.value = savedIds.contains(rifugioId.toLong())

        } catch (e: Exception) {
            android.util.Log.e("CabinViewModel", "Errore nel caricamento dei dati dinamici: ${e.message}")
        }
    }

    /**
     * Alterna lo stato di salvataggio del rifugio
     */
    fun toggleSaveRifugio() {
        viewModelScope.launch {
            try {
                val rifugioId = _rifugio.value?.id ?: return@launch
                val currentlySaved = _isSaved.value ?: false
                val newState = !currentlySaved

                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
                repository.toggleSaveRifugio(userId, rifugioId, newState)

                _isSaved.value = newState
                RifugioSavedEventBus.notifyRifugioSaved()
            } catch (e: Exception) {
                _error.value = "Errore salvataggio: ${e.message}"
            }
        }
    }

    /**
     * Registra una visita al rifugio
     */
    fun recordVisit() {
        viewModelScope.launch {
            try {
                val rifugio = _rifugio.value ?: run {
                    android.util.Log.e("CabinViewModel", "ERRORE: rifugio è null")
                    return@launch
                }

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser == null) {
                    _error.value = "Devi essere loggato per registrare una visita!"
                    return@launch
                }

                val userId = firebaseUser.uid

                val isFirstVisit = pointsRepository.registerRifugioVisitWithPoints(userId, rifugio.id)

                if (isFirstVisit) {
                    val rifugioPoints = getRifugioPoints(rifugio)
                    _successMessage.value =
                        "Prima visita registrata a ${rifugio.nome}! +${rifugioPoints.totalPoints} punti e nuovo timbro!"

                    RifugioSavedEventBus.notifyPointsUpdated(rifugioPoints.totalPoints)
                    RifugioSavedEventBus.notifyUserStatsUpdated()

                } else {
                    _successMessage.value = "Hai già visitato questo rifugio!"
                }

            } catch (e: Exception) {
                _error.value = "Errore imprevisto: ${e.message}"
                android.util.Log.e("CabinViewModel", "Errore in recordVisit: ${e.message}", e)
            }
        }
    }

    /**
     * Carica i punti dell'utente
     */
    private fun loadUserPoints() {
        viewModelScope.launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    val stats = pointsRepository.getUserPointsStats(firebaseUser.uid)
                    _userPoints.value = stats?.totalPoints ?: 0
                    android.util.Log.d("CabinViewModel", "Punti utente aggiornati: ${stats?.totalPoints ?: 0}")
                }
            } catch (e: Exception) {
                android.util.Log.e("CabinViewModel", "Errore caricamento punti: ${e.message}")
            }
        }
    }

    /**
     * Funzioni per sistemare i dati nel layout
     *
     * Periodo di apertura, posti letto, distanza, difficoltà, tempo, descrizione del percorso, accesso al rifugio e i servizi
     */
    fun getOpeningPeriod(rifugio: Rifugio): String {
        return when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> "Stagionale (Giugno - Settembre)"
            TipoRifugio.BIVACCO -> "Sempre aperto"
            TipoRifugio.CAPANNA -> "Estivo (Luglio - Agosto)"
        }
    }

    fun getBeds(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "50 posti letto"
            in 2001..3000 -> "30 posti letto"
            else -> "15 posti letto"
        }
    }

    fun getDistance(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "3.2 km"
            in 2001..3000 -> "5.8 km"
            else -> "8.5 km"
        }
    }

    fun getElevation(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "800m"
            in 2001..3000 -> "1200m"
            else -> "1800m"
        }
    }

    fun getTime(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "2h 30m"
            in 2001..3000 -> "4h 15m"
            else -> "6h 00m"
        }
    }

    fun getDifficulty(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "Difficoltà: Escursionisti [E]"
            in 2001..3000 -> "Difficoltà: Escursionisti Esperti [EE]"
            else -> "Difficoltà: Escursionisti Esperti Attrezzati [EEA]"
        }
    }

    fun getRouteDescription(rifugio: Rifugio): String {
        return when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> "Accesso: ${getRifugioAccess(rifugio)}\n\nIl sentiero è ben segnalato e offre panorami spettacolari durante tutta la salita. Si consiglia di partire di buon mattino."
            TipoRifugio.BIVACCO -> "Accesso libero al bivacco, sempre aperto. Sentiero di montagna con segnaletica CAI. Portare sacco a pelo."
            TipoRifugio.CAPANNA -> "Accesso: ${getRifugioAccess(rifugio)}\n\nPercorso impegnativo ad alta quota. Necessaria esperienza alpinistica e attrezzatura adeguata."
        }
    }

    fun getRifugioAccess(rifugio: Rifugio): String {
        return when (rifugio.localita.lowercase()) {
            "valle d'aosta" -> "Funivia del Monte Bianco"
            "piemonte" -> "Stazione di partenza Alagna"
            "lombardia" -> "Parcheggio di Mandello"
            else -> "Stazione di valle"
        }
    }

    fun hasHotWater(rifugio: Rifugio): Boolean {
        return when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> rifugio.altitudine <= 1000
            TipoRifugio.CAPANNA -> rifugio.altitudine <= 1500
        }
    }

    fun hasShowers(rifugio: Rifugio): Boolean {
        return when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> rifugio.altitudine <= 1000
            TipoRifugio.CAPANNA -> rifugio.altitudine <= 1500
        }
    }

    fun hasElectricity(rifugio: Rifugio): Boolean {
        return when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> rifugio.altitudine <= 1500
            TipoRifugio.CAPANNA -> rifugio.altitudine <= 2500
        }
    }

    fun hasRestaurant(rifugio: Rifugio): Boolean {
        return when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> rifugio.altitudine <= 1200
            TipoRifugio.CAPANNA -> rifugio.altitudine <= 2000
        }
    }

    fun getAverageRating(rifugio: Rifugio): String {
        val reviews = _reviews.value ?: emptyList()
        return if (reviews.isNotEmpty()) {
            "%.1f".format(reviews.map { it.rating }.average())
        } else {
            "0.0"
        }
    }

    fun getReviewCount(rifugio: Rifugio): Int {
        return _reviews.value?.size ?: 0
    }

    /**
     * Ottiene i punti disponibili per questo rifugio
     */
    fun getRifugioPoints(rifugio: Rifugio): RifugioPoints {
        return pointsRepository.getRifugioPoints(rifugio.id, rifugio.altitudine)
    }

    /**
     * Aggiunge una recensione dell'utente
     */
    fun addUserReview(rating: Float, comment: String) {
        viewModelScope.launch {
            try {
                val rifugioId = _rifugio.value?.id ?: return@launch
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch

                android.util.Log.d("CabinViewModel", "ADDING USER REVIEW")
                android.util.Log.d("CabinViewModel", "User ID: ${currentUser.uid}")

                val userInfo = getUserInfoFromFirestore(currentUser.uid)

                val userName = userInfo.first ?: currentUser.displayName
                ?: currentUser.email?.substringBefore("@")?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                } ?: "Utente Anonimo"

                val userAvatar = userInfo.second ?: currentUser.photoUrl?.toString()

                android.util.Log.d("CabinViewModel", "Nome finale: '$userName'")
                android.util.Log.d("CabinViewModel", "Avatar URL: '$userAvatar'")

                val review = Review(
                    rifugioId = rifugioId,
                    userId = currentUser.uid,
                    userName = userName,
                    userAvatar = userAvatar,
                    rating = rating,
                    comment = comment,
                    timestamp = com.google.firebase.Timestamp.now()
                )

                repository.addReview(review)
                kotlinx.coroutines.delay(1000)
                loadDynamicData(rifugioId)

                _successMessage.value = "Recensione aggiunta con successo!"
            } catch (e: Exception) {
                _error.value = "Errore nell'aggiunta della recensione: ${e.message}"
                android.util.Log.e("CabinViewModel", "Errore in addUserReview: ${e.message}", e)
            }
        }
    }

    /**
     * Prende le informazioni dell'utente da Firebase
     */
    private suspend fun getUserInfoFromFirestore(userId: String): Pair<String?, String?> {
        return try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            val nickname = userDoc.getString("nickname")
            val nome = userDoc.getString("nome")
            val cognome = userDoc.getString("cognome")

            val name = when {
                !nickname.isNullOrBlank() -> nickname
                !nome.isNullOrBlank() && !cognome.isNullOrBlank() -> "$nome $cognome"
                !nome.isNullOrBlank() -> nome
                else -> null
            }

            val avatar = userDoc.getString("profileImageUrl")
                ?: userDoc.getString("avatarUrl")
                ?: userDoc.getString("profilePicture")
                ?: userDoc.getString("avatar")

            android.util.Log.d("CabinViewModel", "Dati da Firebase - Nome: '$name', Avatar: '$avatar'")

            Pair(name, avatar)
        } catch (e: Exception) {
            android.util.Log.e("CabinViewModel", "Errore nel recupero dati utente da Firebase: ${e.message}")
            Pair(null, null)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}