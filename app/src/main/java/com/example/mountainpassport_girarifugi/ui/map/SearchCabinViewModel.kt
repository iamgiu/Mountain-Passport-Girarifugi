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
                _rifugi.value = allRifugi
                _filteredRifugi.value = allRifugi
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
        currentUserLocation?.let { userLoc ->
            val distance = calculateDistance(userLoc, rifugio)
            return when {
                distance < 1000 -> "${distance.toInt()} m"
                else -> "${(distance / 1000).toInt()} km"
            }
        }
        return ""
    }

    private val _searchQuery = MutableLiveData<String>()

    fun searchRifugi(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            _filteredRifugi.value = allRifugi
            _hasResults.value = allRifugi.isNotEmpty()
            return
        }

        val filtered = allRifugi.filter { rifugio ->
            rifugio.nome.contains(query, ignoreCase = true) ||
            rifugio.localita.contains(query, ignoreCase = true) ||
            (rifugio.regione?.contains(query, ignoreCase = true) == true)
        }

        _filteredRifugi.value = filtered
        _hasResults.value = filtered.isNotEmpty()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _filteredRifugi.value = allRifugi
        _hasResults.value = allRifugi.isNotEmpty()
    }

    fun clearError() {
        _error.value = null
    }
}