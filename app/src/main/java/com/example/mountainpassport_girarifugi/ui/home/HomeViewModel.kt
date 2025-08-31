package com.example.mountainpassport_girarifugi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import com.example.mountainpassport_girarifugi.data.repository.ActivityRepository
import com.example.mountainpassport_girarifugi.data.repository.FriendActivity
import com.example.mountainpassport_girarifugi.data.repository.ActivityType
import android.content.Context
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.repository.FeedResult
import com.example.mountainpassport_girarifugi.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private var rifugioRepository: RifugioRepository? = null
    private lateinit var activityRepository: ActivityRepository

    // LiveData per l'UI state
    private val _currentTab = MutableLiveData<String>().apply { value = "rifugi" }
    val currentTab: LiveData<String> = _currentTab

    private val _punteggio = MutableLiveData<Int>()
    val punteggio: LiveData<Int> = _punteggio

    private val _rifugiSalvati = MutableLiveData<List<RifugioCard>>()
    val rifugiSalvati: LiveData<List<RifugioCard>> = _rifugiSalvati

    private val _rifugiBonus = MutableLiveData<List<RifugioCard>>()
    val rifugiBonus: LiveData<List<RifugioCard>> = _rifugiBonus

    private val _suggerimentiPersonalizzati = MutableLiveData<List<RifugioCard>>()
    val suggerimentiPersonalizzati: LiveData<List<RifugioCard>> = _suggerimentiPersonalizzati

    // MODIFICATO: Ora usa FriendActivity invece di FeedAmico
    private val _feedAmici = MutableLiveData<List<FeedAmico>>()
    val feedAmici: LiveData<List<FeedAmico>> = _feedAmici

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun setRepository(context: Context) {
        rifugioRepository = RifugioRepository(context)
        activityRepository = ActivityRepository()
        loadData()
    }

    fun setActiveTab(tab: String) {
        _currentTab.value = tab
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadPunteggio()
                loadRifugiSalvati()
                loadRifugiBonus()
                loadSuggerimentiPersonalizzati()
                loadRealFeedAmici() // CAMBIATO: ora carica dati reali
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento dei dati: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     *  Carica il feed reale degli amici dalle attività
     */
    private fun loadRealFeedAmici() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = activityRepository.getFriendsFeed(20)

            _feedAmici.value = when (result) {
                is FeedResult.Success -> {
                    result.activities.map { activity ->
                        when (activity.activityType) {
                            ActivityType.RIFUGIO_VISITATO -> {
                                // 🔹 fallback immagine dal JSON se non arriva da Firestore
                                val rifugioImage = activity.rifugioImageUrl ?: run {
                                    rifugioRepository?.getAllRifugi()
                                        ?.find { it.nome.equals(activity.rifugioName, ignoreCase = true) }
                                        ?.immagineUrl
                                }

                                FeedAmico(
                                    nomeUtente = activity.username,
                                    avatar = activity.userAvatarUrl ?: "ic_account_circle_24",
                                    testoAttivita = "ha visitato un rifugio",
                                    tempo = activity.timeAgo,
                                    tipoAttivita = TipoAttivita.RIFUGIO_VISITATO,
                                    rifugioInfo = RifugioInfo(
                                        nome = activity.rifugioName ?: "Rifugio",
                                        localita = activity.rifugioLocation ?: "Località sconosciuta",
                                        altitudine = activity.rifugioAltitude ?: "0",
                                        puntiGuadagnati = activity.pointsEarned,
                                        immagine = rifugioImage // 🔹 ora può essere URL, drawable name o null
                                    )
                                )
                            }
                            ActivityType.ACHIEVEMENT -> FeedAmico(
                                nomeUtente = activity.username,
                                avatar = activity.userAvatarUrl ?: "ic_account_circle_24",
                                testoAttivita = "ha ottenuto un achievement",
                                tempo = activity.timeAgo,
                                tipoAttivita = TipoAttivita.ACHIEVEMENT
                            )
                            ActivityType.PUNTI_GUADAGNATI -> FeedAmico(
                                nomeUtente = activity.username,
                                avatar = activity.userAvatarUrl ?: "ic_account_circle_24",
                                testoAttivita = "ha guadagnato ${activity.pointsEarned} punti",
                                tempo = activity.timeAgo,
                                tipoAttivita = TipoAttivita.PUNTI_GUADAGNATI
                            )
                            else -> FeedAmico(
                                nomeUtente = activity.username,
                                avatar = activity.userAvatarUrl ?: "ic_account_circle_24",
                                testoAttivita = activity.title,
                                tempo = activity.timeAgo,
                                tipoAttivita = TipoAttivita.GENERIC
                            )
                        }
                    }
                }
                is FeedResult.NoFriends -> emptyList()
                is FeedResult.NoActivities -> emptyList()
                is FeedResult.Error -> {
                    android.util.Log.e("HomeViewModel", "Errore feed amici: ${result.message}")
                    emptyList()
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Metodo per registrare un achievement
     */
    suspend fun logAchievement(
        achievementType: String,
        title: String,
        description: String,
        pointsEarned: Int = 100
    ): Boolean {
        return try {
            activityRepository.logAchievement(
                achievementType = achievementType,
                title = title,
                description = description,
                pointsEarned = pointsEarned
            )
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Errore nel registrare achievement", e)
            false
        }
    }

    /**
     * Refresh del feed amici
     */
    fun refreshFeedAmici() {
        viewModelScope.launch {
            try {
                loadRealFeedAmici()
            } catch (e: Exception) {
                _error.value = "Errore nel ricaricare il feed: ${e.message}"
            }
        }
    }

    /**
     * Fallback ai dati di esempio se non ci sono amici o attività
     */
    private suspend fun loadFallbackFeedAmici() {
        val feed = listOf(
            FeedAmico(
                nomeUtente = "Mario Rossi",
                avatar = "ic_account_circle_24",
                testoAttivita = "ha visitato un rifugio",
                tempo = "2 ore fa",
                tipoAttivita = TipoAttivita.RIFUGIO_VISITATO,
                rifugioInfo = RifugioInfo(
                    nome = "Rifugio Monte Bianco",
                    localita = "Val d'Aosta",
                    altitudine = "2100",
                    puntiGuadagnati = 50,
                    immagine = "rifugio_monte_bianco"
                )
            ),
            FeedAmico(
                nomeUtente = "Lucia Bianchi",
                avatar = "ic_account_circle_24",
                testoAttivita = "ha guadagnato un achievement",
                tempo = "5 ore fa",
                tipoAttivita = TipoAttivita.ACHIEVEMENT
            ),
            FeedAmico(
                nomeUtente = "Giovanni Verde",
                avatar = "ic_account_circle_24",
                testoAttivita = "ha visitato un rifugio",
                tempo = "1 giorno fa",
                tipoAttivita = TipoAttivita.RIFUGIO_VISITATO,
                rifugioInfo = RifugioInfo(
                    nome = "Rifugio Laghi Gemelli",
                    localita = "Val d'Aosta",
                    altitudine = "2134",
                    puntiGuadagnati = 30,
                    immagine = "rifugio_laghi_gemelli"
                )
            )
        )
        _feedAmici.value = feed
    }

    private suspend fun loadRifugiSalvati() {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
            val doc = FirebaseFirestore.getInstance()
                .collection("saved_rifugi")
                .document(userId)
                .get()
                .await()

            val savedIds = doc.get("rifugi") as? List<Long> ?: emptyList()

            val allRifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            val rifugiSalvati = allRifugi.filter { savedIds.contains(it.id.toLong()) }

            _rifugiSalvati.value = rifugiSalvati.map { rifugio ->
                RifugioCard(
                    nome = rifugio.nome,
                    distanza = getDistanceForRifugio(rifugio),
                    altitudine = "${rifugio.altitudine} m",
                    difficolta = getDifficultyForRifugio(rifugio),
                    tempo = getTimeForRifugio(rifugio),
                    immagine = rifugio.immagineUrl ?: "mountain_background"
                )
            }
        } catch (e: Exception) {
            // se c’è un errore, svuoto la lista
            _rifugiSalvati.value = emptyList()
            _error.value = "Errore caricamento rifugi salvati: ${e.message}"
        }
    }

    fun refreshRifugiSalvati() {
        android.util.Log.d("HomeViewModel", "refreshRifugiSalvati() chiamato")
        viewModelScope.launch {
            loadRifugiSalvati()
        }
    }

    private suspend fun loadPunteggio() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val doc = FirebaseFirestore.getInstance()
            .collection("user_points_stats")
            .document(userId)
            .get()
            .await()

        _punteggio.value = (doc.getLong("totalPoints") ?: 0L).toInt()
    }

    private suspend fun loadRifugiBonus() {
        try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            if (rifugi.isNotEmpty()) {
                val rifugiCasuali = rifugi.shuffled().take(4)

                val rifugiBonus = rifugiCasuali.map { rifugio ->
                    RifugioCard(
                        nome = rifugio.nome,
                        distanza = "${(1..10).random()}.${(0..9).random()} km",
                        altitudine = "${rifugio.altitudine} m",
                        difficolta = listOf("Facile", "Medio", "Difficile", "Molto Difficile").random(),
                        tempo = "${(1..8).random()}h ${(0..59).random()}m",
                        immagine = rifugio.immagineUrl ?: "mountain_background",
                        bonusPunti = (50..200).random()
                    )
                }
                _rifugiBonus.value = rifugiBonus
            }
        } catch (e: Exception) {
            val rifugi = listOf(
                RifugioCard(
                    nome = "Rifugio Laghi Gemelli",
                    distanza = "3.2 km",
                    altitudine = "2134 m",
                    difficolta = "Medio",
                    tempo = "2h 15m",
                    immagine = "rifugio_laghi_gemelli",
                    bonusPunti = 75
                ),
                RifugioCard(
                    nome = "Capanna Margherita",
                    distanza = "4.8 km",
                    altitudine = "4554 m",
                    difficolta = "Difficile",
                    tempo = "6h 30m",
                    immagine = "capanna_margherita",
                    bonusPunti = 150
                )
            )
            _rifugiBonus.value = rifugi
        }
    }

    private suspend fun loadSuggerimentiPersonalizzati() {
        try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            if (rifugi.isNotEmpty()) {
                val rifugiBonus = _rifugiBonus.value?.map { it.nome } ?: emptyList()
                val rifugiSuggerimenti = rifugi.filter { !rifugiBonus.contains(it.nome) }.shuffled().take(4)

                val suggerimenti = rifugiSuggerimenti.map { rifugio ->
                    RifugioCard(
                        nome = rifugio.nome,
                        distanza = "${(1..15).random()}.${(0..9).random()} km",
                        altitudine = "${rifugio.altitudine} m",
                        difficolta = listOf("Facile", "Medio", "Difficile", "Molto Difficile").random(),
                        tempo = "${(1..10).random()}h ${(0..59).random()}m",
                        immagine = rifugio.immagineUrl ?: "mountain_background"
                    )
                }
                _suggerimentiPersonalizzati.value = suggerimenti
            }
        } catch (e: Exception) {
            val suggerimenti = listOf(
                RifugioCard(
                    nome = "Rifugio Torino",
                    distanza = "1.8 km",
                    altitudine = "3375 m",
                    difficolta = "Medio",
                    tempo = "3h 10m",
                    immagine = "rifugio_torino"
                )
            )
            _suggerimentiPersonalizzati.value = suggerimenti
        }
    }

    fun refreshData() {
        loadData()
    }

    fun refreshRandomData() {
        viewModelScope.launch {
            try {
                loadRifugiBonus()
                loadSuggerimentiPersonalizzati()
            } catch (e: Exception) {
                _error.value = "Errore nel ricaricamento: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun findRifugioByName(nome: String): Rifugio? {
        return try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            rifugi.find { it.nome == nome }
        } catch (e: Exception) {
            null
        }
    }

    // Utility methods per generare dati coerenti
    private fun getDistanceForRifugio(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "${(1..5).random()}.${(0..9).random()} km"
            in 2001..3000 -> "${(5..10).random()}.${(0..9).random()} km"
            else -> "${(10..15).random()}.${(0..9).random()} km"
        }
    }

    private fun getDifficultyForRifugio(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "Facile"
            in 2001..3000 -> "Medio"
            else -> "Difficile"
        }
    }

    private fun getTimeForRifugio(rifugio: Rifugio): String {
        return when (rifugio.altitudine) {
            in 0..2000 -> "${(1..3).random()}h ${(0..59).random()}m"
            in 2001..3000 -> "${(3..6).random()}h ${(0..59).random()}m"
            else -> "${(6..10).random()}h ${(0..59).random()}m"
        }
    }

    fun getImageResourceName(rifugioNome: String): String {
        return rifugioNome
            .lowercase()
            .replace(" ", "_")
            .replace("à", "a")
            .replace("è", "e")
            .replace("ì", "i")
            .replace("ò", "o")
            .replace("ù", "u")
    }

    // Data classes rimangono uguali
    data class Escursione(
        val nome: String,
        val altitudine: String,
        val distanza: String,
        val id: String? = null,
        val coordinate: String? = null,
        val localita: String? = null,
        val servizi: List<String> = emptyList(),
        val difficolta: String? = null,
        val tempo: String? = null,
        val descrizione: String? = null,
        val immagine: String? = null
    )

    data class RifugioCard(
        val nome: String,
        val distanza: String,
        val altitudine: String,
        val difficolta: String,
        val tempo: String,
        val immagine: String,
        val bonusPunti: Int? = null
    )

    data class FeedAmico(
        val nomeUtente: String,
        val avatar: String,
        val testoAttivita: String,
        val tempo: String,
        val tipoAttivita: TipoAttivita = TipoAttivita.GENERIC,
        val rifugioInfo: RifugioInfo? = null
    )

    enum class TipoAttivita {
        RIFUGIO_VISITATO,
        ACHIEVEMENT,
        PUNTI_GUADAGNATI,
        RECENSIONE,
        GENERIC
    }

    data class RifugioInfo(
        val nome: String,
        val localita: String,
        val altitudine: String,
        val puntiGuadagnati: Int,
        val immagine: String? = null
    )
}