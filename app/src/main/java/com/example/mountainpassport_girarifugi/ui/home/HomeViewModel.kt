package com.example.mountainpassport_girarifugi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

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
        // TODO: Sostituire con chiamata al repository
        val escursione = Escursione(
            nome = "Rifugio Monte Bianco",
            altitudine = "2100 m",
            distanza = "18 km"
        )
        _escursioneProgrammata.value = escursione
    }

    private suspend fun loadPunteggio() {
        // TODO: Sostituire con chiamata al repository per ottenere punteggio utente
        _punteggio.value = 82
    }

    private suspend fun loadRifugiBonus() {
        // TODO: Sostituire con chiamata al repository
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

    private suspend fun loadSuggerimentiPersonalizzati() {
        // TODO: Sostituire con logica per suggerimenti personalizzati basata su preferenze utente
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

    private suspend fun loadFeedAmici() {
        // TODO: Sostituire con chiamata al repository per feed amici
        val feed = listOf(
            FeedAmico("Mario Rossi", "ic_account_circle_24", "ha visitato un rifugio", "2 ore fa"),
            FeedAmico("Lucia Bianchi", "ic_account_circle_24", "ha guadagnato un achievement", "5 ore fa"),
            FeedAmico("Giovanni Verde", "ic_account_circle_24", "ha visitato un rifugio", "1 giorno fa"),
            FeedAmico("Anna Blu", "ic_account_circle_24", "ha lasciato una recensione", "1 giorno fa"),
            FeedAmico("Marco Neri", "ic_account_circle_24", "ha completato 5 rifugi", "2 giorni fa"),
            FeedAmico("Sofia Rosa", "ic_account_circle_24", "ha visitato un rifugio", "3 giorni fa"),
            FeedAmico("Luca Viola", "ic_account_circle_24", "ha guadagnato 150 punti", "4 giorni fa")
        )
        _feedAmici.value = feed
    }

    // Metodi per azioni utente
    fun refreshData() {
        loadData()
    }

    fun onRifugioClicked(rifugio: RifugioCard) {
        // TODO: Logica per navigazione al dettaglio rifugio
        // Potresti emettere un evento di navigazione
    }

    fun onFeedItemClicked(feedItem: FeedAmico) {
        // TODO: Logica per azioni sul feed
    }

    fun clearError() {
        _error.value = null
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

    // Data classes - potresti spostarle in un file separato
    data class Escursione(
        val nome: String,
        val altitudine: String,
        val distanza: String
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
        val avatar: String, // Cambiato da Int a String per flessibilità
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