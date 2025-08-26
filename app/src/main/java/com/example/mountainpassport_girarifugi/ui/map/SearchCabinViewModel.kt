package com.example.mountainpassport_girarifugi.ui.map

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio

class SearchCabinViewModel : ViewModel() {

    private val _rifugi = MutableLiveData<List<Rifugio>>()
    val rifugi: LiveData<List<Rifugio>> = _rifugi

    private val _filteredRifugi = MutableLiveData<List<Rifugio>>()
    val filteredRifugi: LiveData<List<Rifugio>> = _filteredRifugi

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasResults = MutableLiveData<Boolean>()
    val hasResults: LiveData<Boolean> = _hasResults

    private var currentUserLocation: Location? = null
    private var allRifugi: List<Rifugio> = emptyList()

    // Lista dei rifugi di esempio
    private val rifugiEsempio = listOf(
        Rifugio(
            id = 1,
            nome = "Rifugio Torino",
            localita = "Courmayeur, Valle d'Aosta",
            altitudine = 3375,
            latitudine = 45.8467,
            longitudine = 6.8719,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio situato ai piedi del Monte Bianco"
        ),
        Rifugio(
            id = 2,
            nome = "Rifugio Vittorio Sella",
            localita = "Alagna Valsesia, Piemonte",
            altitudine = 2584,
            latitudine = 45.9167,
            longitudine = 7.9333,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio nel cuore del Monte Rosa"
        ),
        Rifugio(
            id = 3,
            nome = "Bivacco della Grigna",
            localita = "Mandello del Lario, Lombardia",
            altitudine = 2184,
            latitudine = 45.9333,
            longitudine = 9.3833,
            tipo = TipoRifugio.BIVACCO,
            descrizione = "Bivacco con vista sul Lago di Como"
        ),
        Rifugio(
            id = 4,
            nome = "Rifugio Laghi Verdi",
            localita = "Gressoney, Valle d'Aosta",
            altitudine = 1850,
            latitudine = 45.8000,
            longitudine = 7.8000,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio circondato da laghi alpini"
        ),
        Rifugio(
            id = 5,
            nome = "Capanna Margherita",
            localita = "Alagna Valsesia, Piemonte",
            altitudine = 4554,
            latitudine = 45.9267,
            longitudine = 7.8783,
            tipo = TipoRifugio.CAPANNA,
            descrizione = "La capanna più alta d'Europa"
        ),
        Rifugio(
            id = 6,
            nome = "Rifugio Bertone",
            localita = "Ceresole Reale, Piemonte",
            altitudine = 2100,
            latitudine = 45.4333,
            longitudine = 7.2167,
            tipo = TipoRifugio.RIFUGIO,
            descrizione = "Rifugio nel Parco Nazionale Gran Paradiso"
        )
    )

    init {
        loadRifugi()
    }

    private fun loadRifugi() {
        _isLoading.value = true

        // Simula il caricamento dei dati
        allRifugi = rifugiEsempio
        _rifugi.value = allRifugi
        _filteredRifugi.value = allRifugi
        _hasResults.value = allRifugi.isNotEmpty()

        _isLoading.value = false
    }

    fun setUserLocation(location: Location) {
        currentUserLocation = location
        sortRifugiByDistance()
    }

    private fun sortRifugiByDistance() {
        currentUserLocation?.let { userLoc ->
            val sortedRifugi = allRifugi.sortedBy { rifugio ->
                calculateDistance(userLoc, rifugio)
            }
            allRifugi = sortedRifugi
            _rifugi.value = sortedRifugi

            // Se c'è una ricerca attiva, riapplica il filtro
            val currentQuery = _searchQuery.value
            if (!currentQuery.isNullOrBlank()) {
                searchRifugi(currentQuery)
            } else {
                _filteredRifugi.value = sortedRifugi
            }
        }
    }

    private fun calculateDistance(userLocation: Location, rifugio: Rifugio): Float {
        val rifugioLocation = Location("rifugio").apply {
            latitude = rifugio.latitudine
            longitude = rifugio.longitudine
        }
        return userLocation.distanceTo(rifugioLocation)
    }

    fun getDistanceToRifugio(rifugio: Rifugio): String {
        return currentUserLocation?.let { userLoc ->
            val distance = calculateDistance(userLoc, rifugio)
            when {
                distance < 1000 -> "${distance.toInt()} m"
                else -> "${"%.1f".format(distance / 1000)} km"
            }
        } ?: ""
    }

    private val _searchQuery = MutableLiveData<String>()

    fun searchRifugi(query: String) {
        _searchQuery.value = query
        _isLoading.value = true

        val filtered = if (query.isBlank()) {
            allRifugi
        } else {
            allRifugi.filter { rifugio ->
                rifugio.nome.contains(query, ignoreCase = true) ||
                        rifugio.localita.contains(query, ignoreCase = true) ||
                        rifugio.tipo.name.contains(query, ignoreCase = true)
            }
        }

        _filteredRifugi.value = filtered
        _hasResults.value = filtered.isNotEmpty()
        _isLoading.value = false
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _filteredRifugi.value = allRifugi
        _hasResults.value = allRifugi.isNotEmpty()
    }

    fun refreshRifugi() {
        loadRifugi()
    }
}