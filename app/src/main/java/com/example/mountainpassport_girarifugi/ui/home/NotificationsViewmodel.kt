package com.example.mountainpassport_girarifugi.ui.notifications

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mountainpassport_girarifugi.data.repository.NotificationsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.tasks.await

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

    // Aggiungi messaggi di successo separati dagli errori
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
        // Pulisci le notifiche obsolete
        cleanupObsoleteNotifications()
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

        // DEBUG: Log per vedere le notifiche
        android.util.Log.d("NotificationsVM", "All notifiche: ${allNotifiche.size}")
        android.util.Log.d("NotificationsVM", "Filtered notifiche: ${filteredNotifiche.size}")

        filteredNotifiche.forEach { notifica ->
            android.util.Log.d("NotificationsVM", "Notifica: ${notifica.titolo} - Tipo: ${notifica.tipo} - Categoria: ${notifica.categoria}")
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

        android.util.Log.d("NotificationsVM", "Recenti: ${recenti.size}, Precedenti: ${precedenti.size}")

        _notificheRecenti.value = recenti
        _notifichePrecedenti.value = precedenti
    }

    // Metodi per azioni utente
    fun refreshNotifiche() {
        loadNotifiche()
    }

    /**
     * Pulisce le notifiche di richieste amicizia obsolete
     */
    private fun cleanupObsoleteNotifications() {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            viewModelScope.launch {
                try {
                    val success = repository.cleanupObsoleteRequests(currentUserId)
                    android.util.Log.d("NotificationsVM", "Cleanup obsolete notifications result: $success")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationsVM", "Error during cleanup", e)
                }
            }
        }
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

    // MODIFICATO: Sposta la notifica in "Precedenti" invece di eliminarla
    private suspend fun moveNotificationToPrevious(notificaId: String): Boolean {
        return try {
            repository.moveNotificationToPrevious(notificaId)
        } catch (e: Exception) {
            android.util.Log.e("NotificationsVM", "Error moving notification to previous", e)
            false
        }
    }

    fun accettaRichiestaAmicizia(senderId: String, notificaId: String) {
        android.util.Log.d("NotificationsVM", "=== ACCEPT FRIEND REQUEST START ===")

        // Validazione input robusta
        if (senderId.isNullOrBlank() || senderId == "null") {
            android.util.Log.e("NotificationsVM", "Invalid senderId: '$senderId'")
            _error.postValue("ID utente non valido")
            return
        }

        if (notificaId.isNullOrBlank()) {
            android.util.Log.e("NotificationsVM", "Invalid notificationId")
            _error.postValue("ID notifica non valido")
            return
        }

        // Verifica che il senderId non sia l'utente corrente
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (senderId == currentUserId) {
            android.util.Log.e("NotificationsVM", "Cannot accept friend request from yourself")
            _error.postValue("Non puoi accettare una richiesta da te stesso")
            return
        }

        // Evita operazioni multiple
        if (_isLoading.value == true) {
            android.util.Log.w("NotificationsVM", "Operation already in progress")
            return
        }

        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val friendRepository = com.example.mountainpassport_girarifugi.data.repository.FriendRepository()

                android.util.Log.d("NotificationsVM", "Calling acceptFriendRequestByUserId with senderId: $senderId")

                friendRepository.acceptFriendRequestByUserId(senderId) { success, error ->
                    android.util.Log.d("NotificationsVM", "Accept callback - Success: $success, Error: $error")

                    _isLoading.postValue(false)

                    if (success) {
                        // MODIFICA: Sposta la notifica in "Precedenti" invece di eliminarla
                        viewModelScope.launch {
                            try {
                                val moveSuccess = moveNotificationToPrevious(notificaId)

                                if (moveSuccess) {
                                    _successMessage.postValue("Richiesta di amicizia accettata!")
                                    android.util.Log.d("NotificationsVM", "Notification moved to previous successfully")
                                } else {
                                    _successMessage.postValue("Richiesta accettata!")
                                    android.util.Log.w("NotificationsVM", "Failed to move notification, but request was accepted")
                                }

                                // NUOVA FUNZIONE: Invia notifica di accettazione al mittente
                                sendAcceptanceNotificationToSender(senderId, currentUserId ?: "")

                            } catch (e: Exception) {
                                android.util.Log.e("NotificationsVM", "Error moving notification", e)
                                _successMessage.postValue("Richiesta accettata!")
                            }
                        }
                    } else {
                        // Gestione errori migliorata
                        val errorMessage = when {
                            error?.contains("già elaborata", ignoreCase = true) == true -> {
                                // Caso speciale: sposta comunque la notifica obsoleta
                                viewModelScope.launch {
                                    try {
                                        moveNotificationToPrevious(notificaId)
                                        _successMessage.postValue("Richiesta già processata - notifica spostata")
                                    } catch (e: Exception) {
                                        _error.postValue("Richiesta già elaborata")
                                    }
                                }
                                return@acceptFriendRequestByUserId
                            }
                            error?.contains("già amici", ignoreCase = true) == true -> {
                                // Già amici: sposta la notifica e mostra successo
                                viewModelScope.launch {
                                    try {
                                        moveNotificationToPrevious(notificaId)
                                        _successMessage.postValue("Siete già amici!")
                                    } catch (e: Exception) {
                                        _successMessage.postValue("Siete già amici!")
                                    }
                                }
                                return@acceptFriendRequestByUserId
                            }
                            error?.contains("non trovata", ignoreCase = true) == true -> {
                                // Richiesta non trovata: sposta la notifica obsoleta
                                viewModelScope.launch {
                                    try {
                                        moveNotificationToPrevious(notificaId)
                                        _successMessage.postValue("Richiesta non più disponibile - notifica spostata")
                                    } catch (e: Exception) {
                                        _error.postValue("Richiesta non più disponibile")
                                    }
                                }
                                return@acceptFriendRequestByUserId
                            }
                            else -> error ?: "Errore nell'accettare la richiesta"
                        }

                        _error.postValue(errorMessage)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("NotificationsVM", "Fatal error in accettaRichiestaAmicizia", e)
                _error.postValue("Errore critico: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    // NUOVA FUNZIONE: Invia notifica di accettazione al mittente
    private suspend fun sendAcceptanceNotificationToSender(senderId: String, accepterId: String) {
        try {
            android.util.Log.d("NotificationsVM", "Sending acceptance notification to sender: $senderId")

            // Ottieni il nome dell'utente che ha accettato
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val accepterDoc = firestore.collection("users")
                .document(accepterId)
                .get()
                .await()

            val accepterUser = accepterDoc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
            val accepterName = if (accepterUser != null) {
                "${accepterUser.nome} ${accepterUser.cognome}".trim()
            } else {
                "Un utente"
            }

            // Crea la notifica di accettazione
            repository.createFriendAcceptedNotification(
                receiverId = senderId,
                accepterName = accepterName,
                accepterId = accepterId
            )

            android.util.Log.d("NotificationsVM", "Acceptance notification sent successfully")

        } catch (e: Exception) {
            android.util.Log.e("NotificationsVM", "Error sending acceptance notification", e)
        }
    }

    fun rifiutaRichiestaAmicizia(senderId: String, notificaId: String) {
        android.util.Log.d("NotificationsVM", "rifiutaRichiestaAmicizia called with senderId: $senderId, notificaId: $notificaId")

        // Validazione input migliorata
        if (senderId.isNullOrBlank() || notificaId.isNullOrBlank()) {
            _error.postValue("Parametri non validi")
            return
        }

        // Verifica che non sia in loading
        if (_isLoading.value == true) {
            android.util.Log.w("NotificationsVM", "Operation already in progress for decline")
            return
        }

        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val friendRepository = com.example.mountainpassport_girarifugi.data.repository.FriendRepository()

                android.util.Log.d("NotificationsVM", "Calling declineFriendRequestByUserId...")

                // Timeout handler
                val timeoutHandler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    _isLoading.postValue(false)
                    _error.postValue("Operazione in timeout")
                }
                timeoutHandler.postDelayed(timeoutRunnable, 30000)

                friendRepository.declineFriendRequestByUserId(senderId) { success, error ->
                    android.util.Log.d("NotificationsVM", "declineFriendRequestByUserId callback - Success: $success, Error: $error")

                    timeoutHandler.removeCallbacks(timeoutRunnable)

                    if (success) {
                        android.util.Log.d("NotificationsVM", "Friend request declined successfully, moving notification...")

                        // MODIFICA: Sposta la notifica in "Precedenti" invece di eliminarla
                        viewModelScope.launch {
                            try {
                                val moveSuccess = moveNotificationToPrevious(notificaId)
                                _isLoading.postValue(false)

                                if (moveSuccess) {
                                    _successMessage.postValue("Richiesta di amicizia rifiutata")
                                } else {
                                    _successMessage.postValue("Richiesta rifiutata")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("NotificationsVM", "Exception moving notification", e)
                                _isLoading.postValue(false)
                                _successMessage.postValue("Richiesta rifiutata")
                            }
                        }
                    } else {
                        android.util.Log.e("NotificationsVM", "Failed to decline friend request: $error")
                        _isLoading.postValue(false)
                        _error.postValue(error ?: "Errore nel rifiutare la richiesta")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationsVM", "Exception in rifiutaRichiestaAmicizia", e)
                _isLoading.postValue(false)
                _error.postValue("Errore nel rifiutare la richiesta: ${e.message}")
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