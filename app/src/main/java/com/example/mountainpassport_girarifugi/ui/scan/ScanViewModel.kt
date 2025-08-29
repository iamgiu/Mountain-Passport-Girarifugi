package com.example.mountainpassport_girarifugi.ui.scan

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.repository.PointsRepository
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ScanViewModel : ViewModel() {
    
    private val TAG = "ScanViewModel"
    
    private val _scanResult = MutableLiveData<ScanResult>()
    val scanResult: LiveData<ScanResult> = _scanResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private var rifugioRepository: RifugioRepository? = null
    private var pointsRepository: PointsRepository? = null
    
    fun initialize(context: Context) {
        rifugioRepository = RifugioRepository(context)
        pointsRepository = PointsRepository(context)
    }
    
    fun processQRCode(qrContent: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                Log.d(TAG, "Processando QR code: $qrContent")
                
                // Parsing del QR code (formato: "rifugio_id")
                val rifugioId = parseQRContent(qrContent)
                if (rifugioId == null) {
                    _scanResult.value = ScanResult.Error("QR code non valido")
                    return@launch
                }
                
                // Trova il rifugio nel database
                val rifugio = findRifugioById(rifugioId)
                if (rifugio == null) {
                    _scanResult.value = ScanResult.Error("Rifugio non trovato")
                    return@launch
                }
                
                // Verifica se l'utente ha già visitato questo rifugio
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    _scanResult.value = ScanResult.Error("Utente non autenticato")
                    return@launch
                }
                
                val hasVisited = pointsRepository?.hasUserVisitedRifugio(userId, rifugioId) ?: false
                if (hasVisited) {
                    _scanResult.value = ScanResult.AlreadyVisited(rifugio)
                    return@launch
                }
                
                // Registra la visita e assegna i punti
                val result = pointsRepository?.recordVisit(userId, rifugioId)
                val pointsEarned = if (result?.isSuccess == true) {
                    result.getOrNull()?.pointsEarned ?: 0
                } else {
                    0
                }
                
                if (pointsEarned > 0) {
                    _scanResult.value = ScanResult.Success(rifugio, pointsEarned)
                } else {
                    _scanResult.value = ScanResult.Error("Errore nella registrazione della visita")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Errore nel processare QR code: ${e.message}")
                _scanResult.value = ScanResult.Error("Errore: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun parseQRContent(qrContent: String): Int? {
        return try {
            // Formato atteso: "rifugio_123" o solo "123"
            val cleanContent = qrContent.trim()
            when {
                cleanContent.startsWith("rifugio_") -> {
                    cleanContent.substringAfter("rifugio_").toIntOrNull()
                }
                cleanContent.matches(Regex("\\d+")) -> {
                    cleanContent.toIntOrNull()
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel parsing QR content: ${e.message}")
            null
        }
    }
    
    private suspend fun findRifugioById(rifugioId: Int): Rifugio? {
        return try {
            val rifugi = rifugioRepository?.getAllRifugi() ?: emptyList()
            rifugi.find { it.id == rifugioId }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel trovare rifugio: ${e.message}")
            null
        }
    }
    
    sealed class ScanResult {
        data class Success(val rifugio: Rifugio, val pointsEarned: Int) : ScanResult()
        data class AlreadyVisited(val rifugio: Rifugio) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }
}