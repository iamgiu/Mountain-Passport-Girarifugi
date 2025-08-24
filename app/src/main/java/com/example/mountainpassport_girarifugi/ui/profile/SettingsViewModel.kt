package com.example.mountainpassport_girarifugi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    // Esempio di LiveData per gestire le impostazioni
    private val _notificationsEnabled = MutableLiveData<Boolean>(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _locationEnabled = MutableLiveData<Boolean>(true)
    val locationEnabled: LiveData<Boolean> = _locationEnabled

    private val _themeMode = MutableLiveData<String>("light")
    val themeMode: LiveData<String> = _themeMode

    // LiveData per gestire il logout
    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        // Qui potresti salvare la preferenza in SharedPreferences o database
        saveNotificationPreference(enabled)
    }

    fun toggleLocation(enabled: Boolean) {
        _locationEnabled.value = enabled
        // Salva la preferenza
        saveLocationPreference(enabled)
    }

    fun setTheme(theme: String) {
        _themeMode.value = theme
        // Salva la preferenza
        saveThemePreference(theme)
    }

    fun performLogout() {
        firebaseAuth.signOut()
        _logoutEvent.value = true
    }

    // Metodo per resettare l'evento di logout dopo averlo gestito
    fun onLogoutEventHandled() {
        _logoutEvent.value = false
    }

    private fun saveNotificationPreference(enabled: Boolean) {
        // TODO: Implementa il salvataggio delle preferenze
        // SharedPreferences o Firebase
    }

    private fun saveLocationPreference(enabled: Boolean) {
        // TODO: Implementa il salvataggio delle preferenze
    }

    private fun saveThemePreference(theme: String) {
        // TODO: Implementa il salvataggio delle preferenze
    }
}