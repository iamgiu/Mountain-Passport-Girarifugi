package com.example.mountainpassport_girarifugi.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

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

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    // Lista completa delle notifiche (per il filtraggio)
    private var allNotifiche: List<Notifica> = emptyList()

    init {
        loadNotifiche()
    }

    fun setActiveFilter(filter: String) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    private fun loadNotifiche() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Simula chiamate al repository/database
                loadAllNotifiche()
                applyFilter(_currentFilter.value ?: "tutte")

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Errore nel caricamento delle notifiche: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadAllNotifiche() {
        // TODO: Sostituire con chiamata al repository
        allNotifiche = listOf(
            // Notifiche recenti (ultime 24 ore)
            Notifica(
                id = "1",
                titolo = "Nuovo rifugio disponibile!",
                descrizione = "Il Rifugio Monte Bianco è ora disponibile per la tua prossima escursione",
                tipo = TipoNotifica.RIFUGIO_NUOVO,
                tempo = "2 ore fa",
                isLetta = false,
                icona = "ic_cabin_24",
                categoria = "rifugi",
                rifugioId = "monte_bianco"
            ),
            Notifica(
                id = "2",
                titolo = "Achievement sbloccato!",
                descrizione = "Hai raggiunto 1000 punti! Complimenti per il traguardo",
                tipo = TipoNotifica.ACHIEVEMENT,
                tempo = "5 ore fa",
                isLetta = false,
                icona = "ic_star_24",
                categoria = "rifugi"
            ),
            Notifica(
                id = "3",
                titolo = "Mario Rossi ti ha seguito",
                descrizione = "Ora potete condividere le vostre escursioni",
                tipo = TipoNotifica.NUOVO_AMICO,
                tempo = "8 ore fa",
                isLetta = true,
                icona = "ic_person_add_24",
                categoria = "amici",
                utenteId = "mario_rossi"
            ),

            // Notifiche precedenti
            Notifica(
                id = "5",
                titolo = "Recensione ricevuta",
                descrizione = "Lucia Bianchi ha lasciato una recensione sulla tua visita al Rifugio Torino",
                tipo = TipoNotifica.RECENSIONE,
                tempo = "2 giorni fa",
                isLetta = true,
                icona = "ic_rate_review_24",
                categoria = "amici",
                rifugioId = "torino",
                utenteId = "lucia_bianchi"
            ),
            Notifica(
                id = "6",
                titolo = "Promemoria escursione",
                descrizione = "Non dimenticare la tua escursione programmata per domani al Rifugio Elisabetta",
                tipo = TipoNotifica.PROMEMORIA,
                tempo = "3 giorni fa",
                isLetta = true,
                icona = "ic_schedule_24",
                categoria = "rifugi",
                rifugioId = "elisabetta"
            ),
            Notifica(
                id = "7",
                titolo = "Sfida completata",
                descrizione = "Hai completato la sfida 'Esploratore delle Alpi' visitando 5 rifugi questo mese",
                tipo = TipoNotifica.SFIDA,
                tempo = "4 giorni fa",
                isLetta = true,
                icona = "ic_emoji_events_24",
                categoria = "rifugi"
            ),
            Notifica(
                id = "8",
                titolo = "Nuovo follower",
                descrizione = "Giovanni Verde ha iniziato a seguire le tue attività",
                tipo = TipoNotifica.NUOVO_AMICO,
                tempo = "5 giorni fa",
                isLetta = true,
                icona = "ic_person_add_24",
                categoria = "amici",
                utenteId = "giovanni_verde"
            )
        )
    }

    private fun applyFilter(filter: String) {
        val filteredNotifiche = when (filter) {
            "rifugi" -> allNotifiche.filter { it.categoria == "rifugi" }
            "amici" -> allNotifiche.filter { it.categoria == "amici" }
            else -> allNotifiche
        }

        // Separa notifiche recenti (ultime 24 ore) e precedenti
        val recenti = filteredNotifiche.filter {
            it.tempo.contains("ore fa") || it.tempo == "Ora"
        }
        val precedenti = filteredNotifiche.filter {
            it.tempo.contains("giorno") || it.tempo.contains("giorni")
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
            TipoNotifica.RIFUGIO_NUOVO, TipoNotifica.PROMEMORIA -> {
                notifica.rifugioId?.let {
                    NavigationEvent.OpenRifugioDetail(it)
                } ?: NavigationEvent.OpenNotificationDetail(notifica.id)
            }
            TipoNotifica.NUOVO_AMICO -> {
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
                // TODO: Chiamata al repository per segnare come letta
                // repository.markNotificationAsRead(notificaId)

                // Aggiorna la lista locale
                allNotifiche = allNotifiche.map { notifica ->
                    if (notifica.id == notificaId) {
                        notifica.copy(isLetta = true)
                    } else {
                        notifica
                    }
                }

                // Riapplica il filtro corrente
                applyFilter(_currentFilter.value ?: "tutte")

            } catch (e: Exception) {
                _error.value = "Errore nell'aggiornamento della notifica: ${e.message}"
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                // TODO: Chiamata al repository per segnare tutte come lette
                // repository.markAllNotificationsAsRead()

                // Aggiorna la lista locale
                allNotifiche = allNotifiche.map { it.copy(isLetta = true) }

                // Riapplica il filtro corrente
                applyFilter(_currentFilter.value ?: "tutte")

            } catch (e: Exception) {
                _error.value = "Errore nell'aggiornamento delle notifiche: ${e.message}"
            }
        }
    }

    fun deleteNotifica(notificaId: String) {
        viewModelScope.launch {
            try {
                // TODO: Chiamata al repository per eliminare la notifica
                // repository.deleteNotification(notificaId)

                // Rimuovi dalla lista locale
                allNotifiche = allNotifiche.filter { it.id != notificaId }

                // Riapplica il filtro corrente
                applyFilter(_currentFilter.value ?: "tutte")

            } catch (e: Exception) {
                _error.value = "Errore nell'eliminazione della notifica: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
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
        val categoria: String, // "rifugi" o "amici"
        val rifugioId: String? = null,
        val utenteId: String? = null,
        val achievementId: String? = null,
        val puntiGuadagnati: Int? = null
    )

    enum class TipoNotifica {
        RIFUGIO_NUOVO,
        RIFUGIO_VISITATO,
        ACHIEVEMENT,
        SFIDA,
        NUOVO_AMICO,
        RECENSIONE,
        PROMEMORIA,
        PUNTI_GUADAGNATI,
        SISTEMA
    }

    sealed class NavigationEvent {
        object OpenSettings : NavigationEvent()
        data class OpenNotificationDetail(val notificaId: String) : NavigationEvent()
        data class OpenRifugioDetail(val rifugioId: String) : NavigationEvent()
        data class OpenUserProfile(val userId: String) : NavigationEvent()
    }
}