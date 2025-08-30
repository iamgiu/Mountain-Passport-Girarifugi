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

// Aggiungi questa classe per la comunicazione tra Fragment
object RifugioSavedEventBus {
    private val _rifugioSavedEvent = MutableLiveData<Unit>()
    val rifugioSavedEvent: LiveData<Unit> = _rifugioSavedEvent

    fun notifyRifugioSaved() {
        _rifugioSavedEvent.value = Unit
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

    // Ottiene l'ID dell'utente corrente autenticato
    private fun getCurrentUserId(): String {
        return UserManager.getCurrentUserIdOrGuest()
    }

    /**
     * Carica i dati del rifugio dall'ID
     */
    fun loadRifugio(rifugioId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val rifugioData = repository.getRifugioById(rifugioId)
                _rifugio.value = rifugioData

                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
                val doc = FirebaseFirestore.getInstance()
                    .collection("saved_rifugi")
                    .document(userId)
                    .get()
                    .await()

                val savedIds = doc.get("rifugi") as? List<Long> ?: emptyList()
                _isSaved.value = savedIds.contains(rifugioId.toLong())

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
            android.util.Log.d("CabinViewModel", "Caricando dati dinamici per rifugio ID: $rifugioId")

            // Carica recensioni (se usi Firebase)
            val reviews = repository.getReviewsForRifugio(rifugioId)
            _reviews.value = reviews

            // Carica statistiche (se usi Firebase)
            val stats = repository.getRifugioStats(rifugioId)
            _stats.value = stats

            // CAMBIATO: Verifica se è salvato usando direttamente UserManager
            val isSaved = UserManager.isRifugioSaved(rifugioId)
            android.util.Log.d("CabinViewModel", "Rifugio $rifugioId salvato: $isSaved")
            _isSaved.value = isSaved

        } catch (e: Exception) {
            android.util.Log.e("CabinViewModel", "Errore nel caricamento dati dinamici: ${e.message}")
        }
    }

    /**
     * Alterna lo stato di salvataggio del rifugio - MODIFICATO
     */
    fun toggleSaveRifugio() {
        viewModelScope.launch {
            try {
                val rifugioId = _rifugio.value?.id ?: return@launch

                val currentlySaved = _isSaved.value ?: false
                val newState = !currentlySaved

                // salva su Firebase
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
                repository.toggleSaveRifugio(userId, rifugioId, newState)

                // aggiorna lo stato locale
                _isSaved.value = newState
                RifugioSavedEventBus.notifyRifugioSaved()
            } catch (e: Exception) {
                _error.value = "Errore salvataggio: ${e.message}"
            }
        }
    }


    /**
     * Funzioni per generare dati di esempio basati sul rifugio
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

    // Metodi per determinare i servizi disponibili
    fun hasHotWater(rifugio: Rifugio): Boolean {
        val result = when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> when (rifugio.altitudine) {
                in 0..1000 -> true
                else -> false
            }

            TipoRifugio.CAPANNA -> when (rifugio.altitudine) {
                in 0..1500 -> true
                else -> false
            }
        }
        android.util.Log.d(
            "CabinViewModel",
            "hasHotWater per ${rifugio.nome} (${rifugio.tipo}, ${rifugio.altitudine}m): $result"
        )
        return result
    }

    fun hasShowers(rifugio: Rifugio): Boolean {
        val result = when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> when (rifugio.altitudine) {
                in 0..1000 -> true
                else -> false
            }

            TipoRifugio.CAPANNA -> when (rifugio.altitudine) {
                in 0..1500 -> true
                else -> false
            }
        }
        android.util.Log.d(
            "CabinViewModel",
            "hasShowers per ${rifugio.nome} (${rifugio.tipo}, ${rifugio.altitudine}m): $result"
        )
        return result
    }

    fun hasElectricity(rifugio: Rifugio): Boolean {
        val result = when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> when (rifugio.altitudine) {
                in 0..1500 -> true
                else -> false
            }

            TipoRifugio.CAPANNA -> when (rifugio.altitudine) {
                in 0..2500 -> true
                else -> false
            }
        }
        android.util.Log.d(
            "CabinViewModel",
            "hasElectricity per ${rifugio.nome} (${rifugio.tipo}, ${rifugio.altitudine}m): $result"
        )
        return result
    }

    fun hasRestaurant(rifugio: Rifugio): Boolean {
        val result = when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> true
            TipoRifugio.BIVACCO -> when (rifugio.altitudine) {
                in 0..1200 -> true
                else -> false
            }

            TipoRifugio.CAPANNA -> when (rifugio.altitudine) {
                in 0..2000 -> true
                else -> false
            }
        }
        android.util.Log.d(
            "CabinViewModel",
            "hasRestaurant per ${rifugio.nome} (${rifugio.tipo}, ${rifugio.altitudine}m): $result"
        )
        return result
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
     * Ottieni i punti disponibili per questo rifugio
     */
    fun getRifugioPoints(rifugio: Rifugio): RifugioPoints {
        return pointsRepository.getRifugioPoints(rifugio.id, rifugio.altitudine)
    }

    /**
     * Registra una visita al rifugio
     */
    fun recordVisit() {
        viewModelScope.launch {
            try {
                val rifugio = _rifugio.value ?: return@launch
                val result = pointsRepository.recordVisit(getCurrentUserId(), rifugio.id)

                result.fold(
                    onSuccess = { userPoints ->
                        _successMessage.value =
                            "Visita registrata! +${userPoints.pointsEarned} punti guadagnati!"
                        android.util.Log.d(
                            "CabinViewModel",
                            "Visita registrata: ${userPoints.pointsEarned} punti"
                        )
                    },
                    onFailure = { exception ->
                        _error.value = "Errore nel registrare la visita: ${exception.message}"
                        android.util.Log.e("CabinViewModel", "Errore visita: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                _error.value = "Errore nel registrare la visita: ${e.message}"
            }
        }
    }

    /**
     * Aggiunge recensioni di test per dimostrare il funzionamento
     */
    fun addTestReviews() {
        android.util.Log.d("CabinViewModel", "addTestReviews() chiamato")
        viewModelScope.launch {
            try {
                val rifugioId = _rifugio.value?.id ?: return@launch
                android.util.Log.d(
                    "CabinViewModel",
                    "Aggiungendo recensioni per rifugio ID: $rifugioId"
                )
                repository.addTestReviews(rifugioId)

                // Aspetta un momento per assicurarsi che i dati siano salvati
                kotlinx.coroutines.delay(1000)

                // Ricarica i dati dinamici
                loadDynamicData(rifugioId)
                android.util.Log.d("CabinViewModel", "Recensioni di test aggiunte con successo")

                // Forza l'aggiornamento dell'UI
                _reviews.value?.let { reviews ->
                    android.util.Log.d("CabinViewModel", "Recensioni aggiornate: ${reviews.size}")
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "CabinViewModel",
                    "Errore nell'aggiunta delle recensioni: ${e.message}"
                )
                _error.value = "Errore nell'aggiunta delle recensioni di test: ${e.message}"
            }
        }
    }

    /**
     * Aggiunge una recensione dell'utente
     */
    fun addUserReview(rating: Float, comment: String) {
        android.util.Log.d(
            "CabinViewModel",
            "addUserReview() chiamato - Rating: $rating, Comment: $comment"
        )
        viewModelScope.launch {
            try {
                val rifugioId = _rifugio.value?.id ?: return@launch
                val userId = "user_${System.currentTimeMillis()}" // ID temporaneo per demo
                val userName =
                    "Utente ${System.currentTimeMillis() % 1000}" // Nome temporaneo per demo

                val review = Review(
                    rifugioId = rifugioId,
                    userId = userId,
                    userName = userName,
                    rating = rating,
                    comment = comment,
                    timestamp = com.google.firebase.Timestamp.now()
                )

                android.util.Log.d(
                    "CabinViewModel",
                    "Aggiungendo recensione utente per rifugio ID: $rifugioId"
                )
                repository.addReview(review)

                // Aspetta un momento per assicurarsi che i dati siano salvati
                kotlinx.coroutines.delay(1000)

                // Ricarica i dati dinamici
                loadDynamicData(rifugioId)
                android.util.Log.d("CabinViewModel", "Recensione utente aggiunta con successo")

                _successMessage.value = "Recensione aggiunta con successo!"
            } catch (e: Exception) {
                android.util.Log.e(
                    "CabinViewModel",
                    "Errore nell'aggiunta della recensione: ${e.message}"
                )
                _error.value = "Errore nell'aggiunta della recensione: ${e.message}"
            }
        }
    }

    /**
     * Pulisce i messaggi di errore
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Pulisce i messaggi di successo
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}