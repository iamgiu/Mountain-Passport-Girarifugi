package com.example.mountainpassport_girarifugi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio
import com.example.mountainpassport_girarifugi.data.model.RifugioPoints
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import com.example.mountainpassport_girarifugi.data.repository.PointsRepository
import com.example.mountainpassport_girarifugi.ui.map.RifugioSavedEventBus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CabinViewModel(application: Application) : AndroidViewModel(application) {

    private val rifugioRepository = RifugioRepository(application)

    private val pointsRepository = PointsRepository(application)

    private val _rifugio = MutableLiveData<RifugioDisplay>()
    val rifugio: LiveData<RifugioDisplay> = _rifugio

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

    private var currentRifugioId: Int = -1

    /**
     * Carica rifugio dal JSON usando l'ID
     */
    fun loadRifugioById(rifugioId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val rifugio = rifugioRepository.getRifugioById(rifugioId)

                if (rifugio != null) {
                    currentRifugioId = rifugioId

                    val rifugioDisplay = convertToDisplay(rifugio)
                    _rifugio.value = rifugioDisplay

                    loadSavedState(rifugioId)
                } else {
                    _error.value = "Rifugio con ID $rifugioId non trovato"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Errore caricamento rifugio: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Carica rifugio dal JSON usando il nome
     */
    fun loadRifugioByName(nome: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val allRifugi = rifugioRepository.getAllRifugi()
                val rifugio = allRifugi.find { it.nome == nome }

                if (rifugio != null) {
                    currentRifugioId = rifugio.id
                    val rifugioDisplay = convertToDisplay(rifugio)
                    _rifugio.value = rifugioDisplay

                    loadSavedState(rifugio.id)
                } else {
                    _error.value = "Rifugio '$nome' non trovato"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Errore ricerca rifugio: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Carica dati direttamente dagli arguments
     */
    fun loadFromArguments(
        nome: String,
        altitudine: String,
        distanza: String,
        localita: String,
        coordinate: String,
        difficolta: String,
        tempo: String,
        descrizione: String
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val allRifugi = rifugioRepository.getAllRifugi()
                val rifugioFromJson = allRifugi.find { it.nome == nome }

                val rifugioDisplay = if (rifugioFromJson != null) {
                    currentRifugioId = rifugioFromJson.id
                    convertToDisplay(rifugioFromJson).copy(
                        distanza = distanza,
                        tempo = tempo,
                        difficolta = difficolta,
                        descrizione = descrizione
                    )
                } else {
                    currentRifugioId = -1
                    RifugioDisplay(
                        id = -1,
                        nome = nome,
                        altitudine = altitudine,
                        localita = localita,
                        coordinate = coordinate,
                        distanza = distanza,
                        tempo = tempo,
                        difficolta = difficolta,
                        descrizione = descrizione,
                        immagineUrl = null,
                        punti = "Punti non disponibili",
                        tipo = TipoRifugio.RIFUGIO
                    )
                }

                _rifugio.value = rifugioDisplay

                if (currentRifugioId != -1) {
                    loadSavedState(currentRifugioId)
                } else {
                    _isSaved.value = false
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Errore caricamento dati: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Carica lo stato salvato da Firebase
     */
    private suspend fun loadSavedState(rifugioId: Int) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
            val doc = FirebaseFirestore.getInstance()
                .collection("saved_rifugi")
                .document(userId)
                .get()
                .await()

            val savedIds = doc.get("rifugi") as? List<Long> ?: emptyList()
            _isSaved.value = savedIds.contains(rifugioId.toLong())
        } catch (e: Exception) {
            _isSaved.value = false
            android.util.Log.e("CabinViewModel", "Errore caricamento stato salvato: ${e.message}")
        }
    }

    /**
     * Converte Rifugio del JSON in RifugioDisplay per la UI
     */
    private fun convertToDisplay(rifugio: Rifugio): RifugioDisplay {
        // Calcola i punti disponibili
        val rifugioPoints = if (rifugio.id > 0) {
            pointsRepository.getRifugioPoints(rifugio.id, rifugio.altitudine)
        } else {
            RifugioPoints(0, 0, false)
        }

        val puntiText = if (rifugioPoints.totalPoints > 0) {
            if (rifugioPoints.isDoublePoints) {
                "+${rifugioPoints.totalPoints} punti (doppi!)"
            } else {
                "+${rifugioPoints.totalPoints} punti"
            }
        } else {
            "Punti non disponibili"
        }

        return RifugioDisplay(
            id = rifugio.id,
            nome = rifugio.nome,
            altitudine = "${rifugio.altitudine}m",
            localita = rifugio.localita,
            coordinate = "${rifugio.latitudine},${rifugio.longitudine}",
            distanza = getDistanceForRifugio(rifugio),
            tempo = getTimeForRifugio(rifugio),
            difficolta = getDifficultyForRifugio(rifugio),
            descrizione = rifugio.descrizione ?: getDescriptionForRifugio(rifugio),
            immagineUrl = rifugio.immagineUrl,
            punti = puntiText,
            tipo = rifugio.tipo
        )
    }

    /**
     * Alterna lo stato di salvataggio del rifugio
     */
    fun toggleSaveRifugio() {
        if (currentRifugioId == -1) {
            _error.value = "Impossibile salvare: rifugio non valido"
            return
        }

        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
                val currentlySaved = _isSaved.value ?: false
                val newState = !currentlySaved

                // Aggiorna Firebase
                val docRef = FirebaseFirestore.getInstance()
                    .collection("saved_rifugi")
                    .document(userId)

                val doc = docRef.get().await()
                val currentList = doc.get("rifugi") as? MutableList<Long> ?: mutableListOf()

                if (newState) {
                    if (!currentList.contains(currentRifugioId.toLong())) {
                        currentList.add(currentRifugioId.toLong())
                    }
                } else {
                    currentList.remove(currentRifugioId.toLong())
                }

                docRef.set(mapOf("rifugi" to currentList)).await()

                // Aggiorna stato locale
                _isSaved.value = newState

                // Notifica altri componenti
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
        if (currentRifugioId == -1) {
            _error.value = "Impossibile registrare visita: rifugio non valido"
            return
        }

        viewModelScope.launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser == null) {
                    _error.value = "Devi essere loggato per registrare una visita!"
                    return@launch
                }

                val userId = firebaseUser.uid

                val alreadyVisited = pointsRepository.hasUserVisitedRifugio(userId, currentRifugioId)
                if (alreadyVisited) {
                    _error.value = "Hai già visitato questo rifugio!"
                    return@launch
                }

                val result = pointsRepository.recordVisit(userId, currentRifugioId)
                result.fold(
                    onSuccess = { userPoints ->
                        _successMessage.value = "Visita registrata! +${userPoints.pointsEarned} punti guadagnati!"
                        RifugioSavedEventBus.notifyPointsUpdated(userPoints.pointsEarned)
                        RifugioSavedEventBus.notifyUserStatsUpdated()
                    },
                    onFailure = { e ->
                        _error.value = "Errore nel registrare la visita: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Errore imprevisto: ${e.message}"
            }
        }
    }

    /**
     * Funzioni per la creazione di dati non presenti nel JSON
     */
    private fun getDistanceForRifugio(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "${(1..5).random()}.${(0..9).random()} km"
            in 2001..3000 -> "${(5..10).random()}.${(0..9).random()} km"
            else -> "${(10..15).random()}.${(0..9).random()} km"
        }
    }

    private fun getTimeForRifugio(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "${(1..3).random()}h ${(0..59).random()}m"
            in 2001..3000 -> "${(3..6).random()}h ${(0..59).random()}m"
            else -> "${(6..10).random()}h ${(0..59).random()}m"
        }
    }

    private fun getDifficultyForRifugio(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "Facile"
            in 2001..3000 -> "Medio"
            else -> "Difficile"
        }
    }

    private fun getDescriptionForRifugio(rifugio: Rifugio): String {
        return when (rifugio.tipo) {
            TipoRifugio.RIFUGIO -> "Il sentiero è ben segnalato e offre panorami spettacolari durante tutta la salita. Si consiglia di partire di buon mattino."
            TipoRifugio.BIVACCO -> "Accesso libero al bivacco, sempre aperto. Sentiero di montagna con segnaletica CAI. Portare sacco a pelo."
            TipoRifugio.CAPANNA -> "Percorso impegnativo ad alta quota. Necessaria esperienza alpinistica e attrezzatura adeguata."
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Data class per rappresentare i dati del rifugio nella UI
     */
    data class RifugioDisplay(
        val id: Int,
        val nome: String,
        val altitudine: String,
        val localita: String,
        val coordinate: String,
        val distanza: String,
        val tempo: String,
        val difficolta: String,
        val descrizione: String,
        val immagineUrl: String?,
        val punti: String,
        val tipo: TipoRifugio
    )
}