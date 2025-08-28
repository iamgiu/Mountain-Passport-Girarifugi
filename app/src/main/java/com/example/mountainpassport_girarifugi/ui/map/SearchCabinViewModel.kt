package com.example.mountainpassport_girarifugi.ui.map

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import kotlinx.coroutines.launch

class SearchCabinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RifugioRepository(application)

    private val _rifugi = MutableLiveData<List<Rifugio>>()
    val rifugi: LiveData<List<Rifugio>> = _rifugi

    private val _filteredRifugi = MutableLiveData<List<Rifugio>>()
    val filteredRifugi: LiveData<List<Rifugio>> = _filteredRifugi

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasResults = MutableLiveData<Boolean>()
    val hasResults: LiveData<Boolean> = _hasResults

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error



    private var currentUserLocation: Location? = null
    private var allRifugi: List<Rifugio> = emptyList()

    init {
        loadRifugi()
    }

    private fun loadRifugi() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Carica tutti i rifugi dal JSON
                allRifugi = repository.getAllRifugi()
                
                // Ordina i rifugi per distanza se la posizione è disponibile
                val sortedRifugi = sortRifugiByDistance(allRifugi)
                allRifugi = sortedRifugi
                
                _rifugi.value = sortedRifugi
                _filteredRifugi.value = sortedRifugi
                _hasResults.value = allRifugi.isNotEmpty()
                
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento dei rifugi: ${e.message}"
                _hasResults.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setUserLocation(location: Location) {
        currentUserLocation = location
        // Riordina tutti i rifugi con la nuova posizione
        val sortedRifugi = sortRifugiByDistance(allRifugi)
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



    private fun sortRifugiByDistance(rifugiList: List<Rifugio>): List<Rifugio> {
        return currentUserLocation?.let { userLoc ->
            rifugiList.sortedBy { rifugio ->
                calculateDistance(userLoc, rifugio)
            }
        } ?: rifugiList
    }

    private fun calculateDistance(userLocation: Location, rifugio: Rifugio): Float {
        val rifugioLocation = Location("rifugio").apply {
            latitude = rifugio.latitudine
            longitude = rifugio.longitudine
        }
        return userLocation.distanceTo(rifugioLocation)
    }

    fun getDistanceToRifugio(rifugio: Rifugio): String {
        currentUserLocation?.let { userLoc ->
            val distance = calculateDistance(userLoc, rifugio)
            return when {
                distance < 1000 -> "${distance.toInt()} m"
                else -> "${(distance / 1000).toInt()} km"
            }
        }
        return "Posizione non disponibile"
    }

    private val _searchQuery = MutableLiveData<String>()

    fun searchRifugi(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            // Se non c'è query, mostra tutti i rifugi ordinati per distanza
            val sortedRifugi = sortRifugiByDistance(allRifugi)
            _filteredRifugi.value = sortedRifugi
            _hasResults.value = allRifugi.isNotEmpty()
            return
        }

        val filtered = allRifugi.filter { rifugio ->
            rifugio.nome.contains(query, ignoreCase = true) ||
            rifugio.localita.contains(query, ignoreCase = true) ||
            (rifugio.regione?.contains(query, ignoreCase = true) == true)
        }

        // Ordina i risultati filtrati per distanza
        val sortedFiltered = sortRifugiByDistance(filtered)
        _filteredRifugi.value = sortedFiltered
        _hasResults.value = filtered.isNotEmpty()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        val sortedRifugi = sortRifugiByDistance(allRifugi)
        _filteredRifugi.value = sortedRifugi
        _hasResults.value = allRifugi.isNotEmpty()
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Aggiorna la posizione dell'utente e riordina i risultati
     */
    fun updateUserLocation(location: Location) {
        setUserLocation(location)
    }

    /**
     * Forza il riordinamento dei risultati attuali per distanza
     */
    fun refreshDistanceSorting() {
        currentUserLocation?.let {
            val currentQuery = _searchQuery.value
            if (!currentQuery.isNullOrBlank()) {
                searchRifugi(currentQuery)
            } else {
                val sortedRifugi = sortRifugiByDistance(allRifugi)
                _filteredRifugi.value = sortedRifugi
            }
        }
    }


}