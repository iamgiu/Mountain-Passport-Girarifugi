package com.example.mountainpassport_girarifugi.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio

class CabinViewModel : ViewModel() {

    private val _rifugio = MutableLiveData<Rifugio>()
    val rifugio: LiveData<Rifugio> = _rifugio

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

    // Lista rifugi di esempio (in una vera app questo verrebbe da un repository)
    private val rifugiEsempio = listOf(
        Rifugio(
            id = 1,
            nome = "Rifugio Torino",
            localita = "Valle d'Aosta",
            altitudine = 3375,
            latitudine = 45.8467,
            longitudine = 6.8719,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio situato ai piedi del Monte Bianco con vista mozzafiato"
        ),
        Rifugio(
            id = 2,
            nome = "Rifugio Vittorio Sella",
            localita = "Piemonte",
            altitudine = 2584,
            latitudine = 45.9167,
            longitudine = 7.9333,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio nel cuore del Monte Rosa"
        ),
        Rifugio(
            id = 3,
            nome = "Bivacco della Grigna",
            localita = "Lombardia",
            altitudine = 2184,
            latitudine = 45.9333,
            longitudine = 9.3833,
            tipo = TipoRifugio.BIVACCO,
            descrizione = "Bivacco con vista sul Lago di Como"
        ),
        Rifugio(
            id = 4,
            nome = "Rifugio Laghi Verdi",
            localita = "Valle d'Aosta",
            altitudine = 1850,
            latitudine = 45.8000,
            longitudine = 7.8000,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio circondato da laghi alpini cristallini"
        ),
        Rifugio(
            id = 5,
            nome = "Capanna Margherita",
            localita = "Piemonte",
            altitudine = 4554,
            latitudine = 45.9267,
            longitudine = 7.8783,
            tipo = TipoRifugio.CAPANNA,
            descrizione = "La capanna più alta d'Europa"
        ),
        Rifugio(
            id = 6,
            nome = "Rifugio Bertone",
            localita = "Piemonte",
            altitudine = 2100,
            latitudine = 45.4333,
            longitudine = 7.2167,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio nel Parco Nazionale Gran Paradiso"
        )
    )

    /**
     * Carica i dati del rifugio dall'ID
     */
    fun loadRifugio(rifugioId: Int) {
        _isLoading.value = true
        _error.value = null

        try {
            // Simula il caricamento dei dati (in una vera app sarà una chiamata al repository)
            val foundRifugio = rifugiEsempio.find { it.id == rifugioId }

            if (foundRifugio != null) {
                _rifugio.value = foundRifugio
                checkIfSaved(rifugioId)
            } else {
                _error.value = "Rifugio non trovato"
            }
        } catch (e: Exception) {
            _error.value = "Errore nel caricamento dei dati: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Verifica se il rifugio è salvato nei preferiti
     */
    private fun checkIfSaved(rifugioId: Int) {
        // Simula il controllo nei preferiti (in una vera app sarà una chiamata al repository/database)
        _isSaved.value = false // Per ora sempre false
    }

    /**
     * Alterna lo stato di salvataggio del rifugio
     */
    fun toggleSaveRifugio() {
        val currentState = _isSaved.value ?: false
        _isSaved.value = !currentState

        // Qui andresti a salvare/rimuovere il rifugio dal database/repository
        // Per ora è solo locale
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

    fun getAverageRating(rifugio: Rifugio): String {
        return when (rifugio.id % 3) {
            0 -> "4.8"
            1 -> "4.5"
            else -> "4.2"
        }
    }

    fun getReviewCount(rifugio: Rifugio): Int {
        return when (rifugio.id % 4) {
            0 -> 45
            1 -> 23
            2 -> 67
            else -> 31
        }
    }

    /**
     * Pulisce i messaggi di errore
     */
    fun clearError() {
        _error.value = null
    }
}