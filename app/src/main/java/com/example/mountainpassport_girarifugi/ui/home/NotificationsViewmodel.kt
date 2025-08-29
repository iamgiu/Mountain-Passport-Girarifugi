package com.example.mountainpassport_girarifugi.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.data.repository.NotificationsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

class NotificationsViewModel : ViewModel() {

    private val repository = NotificationsRepository()

    // LiveData per l'UI state
    private val _currentFilter = MutableLiveData<String>().apply { value = "tutte" }
    val currentFilter: LiveData<String> = _currentFilter

    private val _notificheRecenti = MutableLiveData<List<Notifica>>()
    val notificheRecenti: LiveData<List<Notifica>> = _notificheRecenti

    private val _notifichePrecedenti = MutableLiveData<List<Notifica>>()
    val notifichePrecedenti: LiveData<List<Notifica>> = _notifichePrecedenti

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // FIX: Aggiungi messaggi di successo separati dagli errori
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    private val _azioniPending = MutableLiveData<List<AzioneNotifica>>()
    val azioniPending: LiveData<List<AzioneNotifica>> = _azioniPending

    // Lista completa delle notifiche (per il filtraggio)
    private var allNotifiche: List<Notifica> = emptyList()

    init {
        // Carica le notifiche all'avvio
        loadNotifiche()
        // Inizia l'osservazione in tempo reale
        observeNotifications()
    }

    fun setActiveFilter(filter: String) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    /**
     * Carica le notifiche dal repository
     */
    private fun loadNotifiche() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                allNotifiche = repository.getAllNotifications()
                applyFilter(_currentFilter.value ?: "tutte")
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento delle notifiche: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Osserva le notifiche in tempo reale
     */
    private fun observeNotifications() {
        viewModelScope.launch {
            repository.observeNotifications()
                .catch { e ->
                    _error.value = "Errore nell'osservazione delle notifiche: ${e.message}"
                }
                .collect { notifiche ->
                    allNotifiche = notifiche
                    applyFilter(_currentFilter.value ?: "tutte")
                }
        }
    }

    private fun applyFilter(filter: String) {
        val filteredNotifiche = when (filter) {
            "rifugi" -> allNotifiche.filter { it.categoria == "rifugi" }
            "amici" -> allNotifiche.filter { it.categoria == "amici" }
            else -> allNotifiche
        }

        // Separa notifiche recenti (ultime 24 ore) e precedenti
        val recenti = filteredNotifiche.filter {
            it.tempo.contains("ore fa") ||
                    it.tempo.contains("minuti fa") ||
                    it.tempo == "Ora"
        }
        val precedenti = filteredNotifiche.filter {
            it.tempo.contains("giorno") ||
                    it.tempo.contains("giorni") ||
                    it.tempo.contains("mesi fa")
        }

        _notificheRecenti.value = recenti
        _notifichePrecedenti.value = precedenti
    }

    // Metodi per azioni utente
    fun refreshNotifiche() {
        loadNotifiche()
    }

    fun onNotificaClicked(notifica: Notifica) {
        // Segna come letta se non lo è già
        if (!notifica.isLetta) {
            markAsRead(notifica.id)
        }

        // Naviga in base al tipo di notifica
        val navigationEvent = when (notifica.tipo) {
            TipoNotifica.NUOVO_MEMBRO_GRUPPO -> {
                notifica.rifugioId?.let {
                    NavigationEvent.OpenRifugioDetail(it)
                } ?: NavigationEvent.OpenNotificationDetail(notifica.id)
            }
            TipoNotifica.RICHIESTA_AMICIZIA -> {
                notifica.utenteId?.let {
                    NavigationEvent.OpenUserProfile(it)
                } ?: NavigationEvent.OpenNotificationDetail(notifica.id)
            }
            else -> NavigationEvent.OpenNotificationDetail(notifica.id)
        }

        _navigationEvent.value = navigationEvent
    }

    fun onImpostazioniClicked() {
        _navigationEvent.value = NavigationEvent.OpenSettings
    }

    fun markAsRead(notificaId: String) {
        viewModelScope.launch {
            try {
                val success = repository.markAsRead(notificaId)
                if (!success) {
                    _error.value = "Errore nell'aggiornamento della notifica"
                }
                // L'aggiornamento della UI avverrà automaticamente tramite l'observer
            } catch (e: Exception) {
                _error.value = "Errore nell'aggiornamento della notifica: ${e.message}"
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val success = repository.markAllAsRead()
                if (!success) {
                    _error.value = "Errore nell'aggiornamento delle notifiche"
                }
                // L'aggiornamento della UI avverrà automaticamente tramite l'observer
            } catch (e: Exception) {
                _error.value = "Errore nell'aggiornamento delle notifiche: ${e.message}"
            }
        }
    }

    fun deleteNotifica(notificaId: String) {
        viewModelScope.launch {
            try {
                val success = repository.deleteNotification(notificaId)
                if (!success) {
                    _error.value = "Errore nell'eliminazione della notifica"
                }
                // L'aggiornamento della UI avverrà automaticamente tramite l'observer
            } catch (e: Exception) {
                _error.value = "Errore nell'eliminazione della notifica: ${e.message}"
            }
        }
    }

    fun accettaRichiestaAmicizia(senderId: String, notificaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val friendRepository = com.example.mountainpassport_girarifugi.data.repository.FriendRepository()

                friendRepository.acceptFriendRequestByUserId(senderId) { success, error ->
                    viewModelScope.launch {
                        if (success) {
                            // Elimina la notifica dopo aver accettato
                            deleteNotifica(notificaId)
                            _successMessage.value = "Richiesta di amicizia accettata!"
                            _error.value = null
                        } else {
                            _error.value = error ?: "Errore nell'accettare la richiesta"
                        }
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = "Errore nell'accettare la richiesta: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun rifiutaRichiestaAmicizia(senderId: String, notificaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val friendRepository = com.example.mountainpassport_girarifugi.data.repository.FriendRepository()

                friendRepository.declineFriendRequestByUserId(senderId) { success, error ->
                    viewModelScope.launch {
                        if (success) {
                            // Elimina la notifica dopo aver rifiutato
                            deleteNotifica(notificaId)
                            _successMessage.value = "Richiesta di amicizia rifiutata"
                            _error.value = null
                        } else {
                            _error.value = error ?: "Errore nel rifiutare la richiesta"
                        }
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = "Errore nel rifiutare la richiesta: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    // Data classes
    data class Notifica(
        val id: String,
        val titolo: String,
        val descrizione: String,
        val tipo: TipoNotifica,
        val tempo: String,
        val isLetta: Boolean,
        val icona: String,
        val categoria: String, // "rifugi", "amici", "punti", etc.
        val rifugioId: String? = null,
        val utenteId: String? = null,
        val achievementId: String? = null,
        val puntiGuadagnati: Int? = null,
        val friendRequestId: String? = null
    )

    enum class TipoNotifica {
        RICHIESTA_AMICIZIA,
        NUOVO_MEMBRO_GRUPPO,
        NUOVA_SFIDA_MESE,
        DOPPIO_PUNTI_RIFUGI,
        SFIDA_COMPLETATA,
        PUNTI_OTTENUTI,
        TIMBRO_OTTENUTO,
        SISTEMA
    }

    data class NotificaRichiestaAmicizia(
        val notifica: Notifica,
        val richiedenteId: String,
        val richiedenteNome: String
    )

    sealed class AzioneNotifica {
        data class AccettaAmicizia(val richiedenteId: String) : AzioneNotifica()
        data class RifiutaAmicizia(val richiedenteId: String) : AzioneNotifica()
        data class VediSfida(val sfidaId: String) : AzioneNotifica()
        data class VediRifugi(val categoria: String) : AzioneNotifica()
        data class VediGruppo(val gruppoId: String) : AzioneNotifica()
    }

    sealed class NavigationEvent {
        object OpenSettings : NavigationEvent()
        data class OpenNotificationDetail(val notificaId: String) : NavigationEvent()
        data class OpenRifugioDetail(val rifugioId: String) : NavigationEvent()
        data class OpenUserProfile(val userId: String) : NavigationEvent()
    }
}