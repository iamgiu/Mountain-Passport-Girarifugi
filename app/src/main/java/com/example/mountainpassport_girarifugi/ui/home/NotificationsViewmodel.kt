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


    private val _currentFilter = MutableLiveData<String>().apply { value = "tutte" }
    val currentFilter: LiveData<String> = _currentFilter

    /**
     *
     * Due liste separate notiifiche recenti e quelle più vecchie
     *
     */
    private val _notificheRecenti = MutableLiveData<List<Notifica>>()
    val notificheRecenti: LiveData<List<Notifica>> = _notificheRecenti

    private val _notifichePrecedenti = MutableLiveData<List<Notifica>>()
    val notifichePrecedenti: LiveData<List<Notifica>> = _notifichePrecedenti

    /**
     *
     * Indica se c'è un'operazione in corso
     *
     */
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    private val _azioniPending = MutableLiveData<List<AzioneNotifica>>()
    val azioniPending: LiveData<List<AzioneNotifica>> = _azioniPending

    private var allNotifiche: List<Notifica> = emptyList()

    /**
     *
     * Inizializzazione
     *
     */
    init {
        loadNotifiche()
        cleanupObsoleteNotifications()
        observeNotifications()
    }

    /**
     *
     * Gestione dei filtri
     *
     */
    fun setActiveFilter(filter: String) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    /**
     *
     * Caricamento delle notifiche
     *
     */
    private fun loadNotifiche() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                allNotifiche = repository.getAllNotifications()
                applyFilter(_currentFilter.value ?: "tutte")
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     *
     * Observer delle notifiche
     *
     * Ogni volta che arrivano nuova notifiche aggiorna la lista e applica il filtro
     */
    private fun observeNotifications() {
        viewModelScope.launch {
            repository.observeNotifications()
                .catch {
                }
                .collect { notifiche ->
                    allNotifiche = notifiche
                    applyFilter(_currentFilter.value ?: "tutte")
                }
        }
    }

    /**
     *
     * Applica il filtro alle notifiche
     *
     */
    private fun applyFilter(filter: String) {
        val filteredNotifiche = when (filter) {
            "rifugi" -> allNotifiche.filter {
                it.tipo == TipoNotifica.NUOVO_MEMBRO_GRUPPO ||
                        it.tipo == TipoNotifica.NUOVA_SFIDA_MESE ||
                        it.tipo == TipoNotifica.DOPPIO_PUNTI_RIFUGI ||
                        it.tipo == TipoNotifica.SFIDA_COMPLETATA ||
                        it.tipo == TipoNotifica.PUNTI_OTTENUTI ||
                        it.tipo == TipoNotifica.TIMBRO_OTTENUTO ||
                        it.tipo == TipoNotifica.SISTEMA
            }
            "amici" -> allNotifiche.filter { it.tipo == TipoNotifica.RICHIESTA_AMICIZIA }
            "tutte" -> allNotifiche
            else -> allNotifiche
        }

        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)

        val recenti = filteredNotifiche.filter { (it.timestamp ?: 0L) >= oneDayAgo }
        val precedenti = filteredNotifiche.filter { (it.timestamp ?: 0L) < oneDayAgo }

        _notificheRecenti.value = recenti.sortedByDescending { it.timestamp ?: 0L }
        _notifichePrecedenti.value = precedenti.sortedByDescending { it.timestamp ?: 0L }
    }


    /**
     *
     * Ricarica le notifiche
     *
     */
    fun refreshNotifiche() {
        loadNotifiche()
    }

    /**
     *
     * Pulisce dalla notifiche obsolete
     *
     */
    private fun cleanupObsoleteNotifications() {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            viewModelScope.launch {
                repository.cleanupObsoleteRequests(currentUserId)
            }
        }
    }

    /**
     *
     * Se la notifica non è letta quando l'utente ci clicca la marca come letta
     *
     */
    fun onNotificaClicked(notifica: Notifica) {
        if (!notifica.isLetta) {
            markAsRead(notifica.id)
        }

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

    /**
     *
     * Segna la notifica come letta
     *
     */
    fun markAsRead(notificaId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificaId)
        }
    }

    /**
     *
     * Segna tutte le notifiche come lette
     *
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    /**
     *
     * Elimina una notifica
     *
     */
    fun deleteNotifica(notificaId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificaId)
        }
    }

    /**
     *
     * Accetta le richieste di amicizia
     *
     */
    fun accettaRichiestaAmicizia(senderId: String, notificaId: String) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (senderId.isBlank() || senderId == currentUserId) return
        if (notificaId.isBlank()) return

        if (_isLoading.value == true) return

        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val friendRepository = com.example.mountainpassport_girarifugi.data.repository.FriendRepository()
                friendRepository.acceptFriendRequestByUserId(senderId) { success, _ ->
                    _isLoading.postValue(false)
                    if (success) {
                        viewModelScope.launch {
                            repository.deleteNotification(notificaId)
                            sendAcceptanceNotificationToSender(senderId, currentUserId ?: "")
                        }
                    } else {
                        viewModelScope.launch {
                            repository.deleteNotification(notificaId)
                        }
                    }
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     *
     * Chiede al FriendRepository di accettare la richiesta
     *
     * Se va bene elimina la notifica e invia una nuova notifica al mittente dicendo che l'amicizia è stata accettata
     *
     */
    private suspend fun sendAcceptanceNotificationToSender(senderId: String, accepterId: String) {
        try {
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

            repository.createFriendAcceptedNotification(
                receiverId = senderId,
                accepterName = accepterName,
                accepterId = accepterId
            )
        } catch (_: Exception) {
        }
    }

    /**
     *
     * Rifiuta la richiesta di amicizia
     *
     */
    fun rifiutaRichiestaAmicizia(senderId: String, notificaId: String) {
        if (senderId.isBlank() || notificaId.isBlank()) return
        if (_isLoading.value == true) return

        _isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val friendRepository = com.example.mountainpassport_girarifugi.data.repository.FriendRepository()
                val timeoutHandler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    _isLoading.postValue(false)
                }
                timeoutHandler.postDelayed(timeoutRunnable, 30000)

                friendRepository.declineFriendRequestByUserId(senderId) { success, _ ->
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    _isLoading.postValue(false)
                    if (success) {
                        viewModelScope.launch {
                            repository.deleteNotification(notificaId)
                        }
                    }
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    /**
     *
     * Data class che rappresenta la notifica
     *
     */
    data class Notifica(
        val id: String,
        val titolo: String,
        val descrizione: String,
        val tipo: TipoNotifica,
        val tempo: String,
        val isLetta: Boolean,
        val icona: String,
        val rifugioId: String? = null,
        val utenteId: String? = null,
        val achievementId: String? = null,
        val puntiGuadagnati: Int? = null,
        val friendRequestId: String? = null,
        val avatarUrl: String? = null,
        val timestamp: Long? = null
    )

    /**
     *
     * Enum class che rappresenta il tipo di Notifica
     *
     * Per ora: Richiesta amicizia, Punti ottenuti, Timbro ottenuto, Sistema
     *
     */
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

    /**
     *
     * Data class per rappresentare la notifica di Richiesta di amicizia
     *
     */
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