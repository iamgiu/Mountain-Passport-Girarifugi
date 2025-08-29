package com.example.mountainpassport_girarifugi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import android.content.Context

class HomeViewModel : ViewModel() {

    private var rifugioRepository: RifugioRepository? = null

    // LiveData per l'UI state
    private val _currentTab = MutableLiveData<String>().apply { value = "rifugi" }
    val currentTab: LiveData<String> = _currentTab

    private val _escursioneProgrammata = MutableLiveData<Escursione>()
    val escursioneProgrammata: LiveData<Escursione> = _escursioneProgrammata

    private val _punteggio = MutableLiveData<Int>()
    val punteggio: LiveData<Int> = _punteggio

    private val _rifugiBonus = MutableLiveData<List<RifugioCard>>()
    val rifugiBonus: LiveData<List<RifugioCard>> = _rifugiBonus

    private val _suggerimentiPersonalizzati = MutableLiveData<List<RifugioCard>>()
    val suggerimentiPersonalizzati: LiveData<List<RifugioCard>> = _suggerimentiPersonalizzati

    private val _feedAmici = MutableLiveData<List<FeedAmico>>()
    val feedAmici: LiveData<List<FeedAmico>> = _feedAmici

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // loadData() verrà chiamato dopo aver impostato il repository
    }
    
    fun setRepository(context: Context) {
        rifugioRepository = RifugioRepository(context)
        loadData()
    }

    fun setActiveTab(tab: String) {
        _currentTab.value = tab
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Simula chiamate al repository/database
                loadEscursioneProgrammata()
                loadPunteggio()
                loadRifugiBonus()
                loadSuggerimentiPersonalizzati()
                loadFeedAmici()

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento dei dati: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadEscursioneProgrammata() {
        try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            if (rifugi.isNotEmpty()) {
                // Seleziona un rifugio casuale per l'escursione programmata
                val rifugioCasuale = rifugi.random()
                
                val escursione = Escursione(
                    nome = rifugioCasuale.nome,
                    altitudine = "${rifugioCasuale.altitudine} m",
                    distanza = "${(5..25).random()} km", // Distanza casuale
                    id = rifugioCasuale.id.toString(),
                    coordinate = "${rifugioCasuale.latitudine},${rifugioCasuale.longitudine}",
                    localita = rifugioCasuale.localita,
                    servizi = listOf("Acqua calda", "Luce elettrica", "Docce", "Ristorante"),
                    difficolta = "Escursionisti [E]",
                    tempo = "${(2..8).random()}h ${(0..59).random()}m",
                    descrizione = rifugioCasuale.descrizione,
                    immagine = rifugioCasuale.immagineUrl // Usa l'URL dal JSON
                )
                _escursioneProgrammata.value = escursione
            }
        } catch (e: Exception) {
            // Fallback con dati di default
            val escursione = Escursione(
                nome = "Rifugio Monte Bianco",
                altitudine = "2100 m",
                distanza = "18 km",
                id = "monte_bianco",
                coordinate = "45.8326,6.8652",
                localita = "Val d'Aosta",
                servizi = listOf("Acqua calda", "Luce elettrica", "Docce", "Ristorante"),
                difficolta = "Escursionisti [E]",
                tempo = "5h 30m",
                descrizione = "Un rifugio panoramico con vista mozzafiato sul Monte Bianco",
                immagine = "rifugio_monte_bianco"
            )
            _escursioneProgrammata.value = escursione
        }
    }

    private suspend fun loadPunteggio() {
        // TODO: Sostituire con chiamata al repository per ottenere punteggio utente
        _punteggio.value = 82
    }

    private suspend fun loadRifugiBonus() {
        try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            if (rifugi.isNotEmpty()) {
                // Seleziona 4 rifugi casuali per i rifugi bonus
                val rifugiCasuali = rifugi.shuffled().take(4)
                
                val rifugiBonus = rifugiCasuali.map { rifugio ->
                    RifugioCard(
                        nome = rifugio.nome,
                        distanza = "${(1..10).random()}.${(0..9).random()} km",
                        altitudine = "${rifugio.altitudine} m",
                        difficolta = listOf("Facile", "Medio", "Difficile", "Molto Difficile").random(),
                        tempo = "${(1..8).random()}h ${(0..59).random()}m",
                        immagine = rifugio.immagineUrl ?: "mountain_background", // Usa l'URL dal JSON o fallback
                        bonusPunti = (50..200).random()
                    )
                }
                _rifugiBonus.value = rifugiBonus
            }
        } catch (e: Exception) {
            // Fallback con dati di default
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
                ),
                RifugioCard(
                    nome = "Rifugio Città di Milano",
                    distanza = "2.1 km",
                    altitudine = "1890 m",
                    difficolta = "Facile",
                    tempo = "1h 45m",
                    immagine = "rifugio_citta_di_milano",
                    bonusPunti = 50
                ),
                RifugioCard(
                    nome = "Rifugio Gnifetti",
                    distanza = "5.5 km",
                    altitudine = "3647 m",
                    difficolta = "Difficile",
                    tempo = "4h 20m",
                    immagine = "rifugio_gnifetti",
                    bonusPunti = 120
                )
            )
            _rifugiBonus.value = rifugi
        }
    }

    private suspend fun loadSuggerimentiPersonalizzati() {
        try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            if (rifugi.isNotEmpty()) {
                // Seleziona 4 rifugi casuali diversi dai rifugi bonus per i suggerimenti
                val rifugiBonus = _rifugiBonus.value?.map { it.nome } ?: emptyList()
                val rifugiSuggerimenti = rifugi.filter { !rifugiBonus.contains(it.nome) }.shuffled().take(4)
                
                val suggerimenti = rifugiSuggerimenti.map { rifugio ->
                    RifugioCard(
                        nome = rifugio.nome,
                        distanza = "${(1..15).random()}.${(0..9).random()} km",
                        altitudine = "${rifugio.altitudine} m",
                        difficolta = listOf("Facile", "Medio", "Difficile", "Molto Difficile").random(),
                        tempo = "${(1..10).random()}h ${(0..59).random()}m",
                        immagine = rifugio.immagineUrl ?: "mountain_background" // Usa l'URL dal JSON o fallback
                    )
                }
                _suggerimentiPersonalizzati.value = suggerimenti
            }
        } catch (e: Exception) {
            // Fallback con dati di default
            val suggerimenti = listOf(
                RifugioCard(
                    nome = "Rifugio Torino",
                    distanza = "1.8 km",
                    altitudine = "3375 m",
                    difficolta = "Medio",
                    tempo = "3h 10m",
                    immagine = "rifugio_torino"
                ),
                RifugioCard(
                    nome = "Rifugio Elisabetta",
                    distanza = "2.7 km",
                    altitudine = "2195 m",
                    difficolta = "Facile",
                    tempo = "1h 50m",
                    immagine = "rifugio_elisabetta"
                ),
                RifugioCard(
                    nome = "Rifugio Vittorio Sella",
                    distanza = "3.9 km",
                    altitudine = "2584 m",
                    difficolta = "Medio",
                    tempo = "2h 45m",
                    immagine = "rifugio_vittorio_sella"
                ),
                RifugioCard(
                    nome = "Capanna Regina Margherita",
                    distanza = "6.2 km",
                    altitudine = "4554 m",
                    difficolta = "Molto Difficile",
                    tempo = "7h 00m",
                    immagine = "capanna_regina_margherita"
                )
            )
            _suggerimentiPersonalizzati.value = suggerimenti
        }
    }

    private suspend fun loadFeedAmici() {
        val feed = listOf(
            // Rifugio visitato
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
            // Achievement
            FeedAmico(
                nomeUtente = "Lucia Bianchi",
                avatar = "ic_account_circle_24",
                testoAttivita = "ha guadagnato un achievement",
                tempo = "5 ore fa",
                tipoAttivita = TipoAttivita.ACHIEVEMENT
                // rifugioInfo lasciato null
            ),
            // Altro rifugio visitato
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
            ),
            // Punti guadagnati
            FeedAmico(
                nomeUtente = "Luca Viola",
                avatar = "ic_account_circle_24",
                testoAttivita = "ha guadagnato 150 punti",
                tempo = "4 giorni fa",
                tipoAttivita = TipoAttivita.PUNTI_GUADAGNATI
            )
        )
        _feedAmici.value = feed
    }


    // Metodi per azioni utente
    fun refreshData() {
        loadData()
    }
    
    fun refreshRandomData() {
        // Ricarica solo i dati casuali (escursione, rifugi bonus, suggerimenti)
        viewModelScope.launch {
            try {
                loadEscursioneProgrammata()
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
    
    suspend fun findRifugioByName(nome: String): com.example.mountainpassport_girarifugi.data.model.Rifugio? {
        return try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            rifugi.find { it.nome == nome }
        } catch (e: Exception) {
            null
        }
    }

    // Utility methods
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