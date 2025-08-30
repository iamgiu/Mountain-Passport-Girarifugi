package com.example.mountainpassport_girarifugi.utils

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREFS_NAME = "mountain_passport_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_SAVED_RIFUGI = "saved_rifugi"
    private const val GUEST_USER_ID = "guest_user"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Genera un ID utente unico se non esiste
        if (getCurrentUserId().isEmpty()) {
            val newUserId = "user_${System.currentTimeMillis()}"
            setCurrentUserId(newUserId)
        }
    }

    fun getCurrentUserId(): String {
        return sharedPreferences?.getString(KEY_USER_ID, "") ?: ""
    }

    fun getCurrentUserIdOrGuest(): String {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) userId else GUEST_USER_ID
    }

    private fun setCurrentUserId(userId: String) {
        sharedPreferences?.edit()?.putString(KEY_USER_ID, userId)?.apply()
    }

    fun clearUser() {
        sharedPreferences?.edit()?.remove(KEY_USER_ID)?.apply()
    }

    // Nuovi metodi per gestire i rifugi salvati
    fun getSavedRifugiIds(): Set<Int> {
        val savedString = sharedPreferences?.getString(KEY_SAVED_RIFUGI, "") ?: ""
        return if (savedString.isEmpty()) {
            emptySet()
        } else {
            savedString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    fun isRifugioSaved(rifugioId: Int): Boolean {
        return getSavedRifugiIds().contains(rifugioId)
    }

    fun saveRifugio(rifugioId: Int) {
        val currentSaved = getSavedRifugiIds().toMutableSet()
        currentSaved.add(rifugioId)
        saveSavedRifugiIds(currentSaved)
    }

    fun removeSavedRifugio(rifugioId: Int) {
        val currentSaved = getSavedRifugiIds().toMutableSet()
        currentSaved.remove(rifugioId)
        saveSavedRifugiIds(currentSaved)
    }

    fun toggleSaveRifugio(rifugioId: Int): Boolean {
        val isCurrentlySaved = isRifugioSaved(rifugioId)
        if (isCurrentlySaved) {
            removeSavedRifugio(rifugioId)
        } else {
            saveRifugio(rifugioId)
        }
        return !isCurrentlySaved // Ritorna il nuovo stato
    }

    private fun saveSavedRifugiIds(rifugiIds: Set<Int>) {
        val savedString = rifugiIds.joinToString(",")
        sharedPreferences?.edit()?.putString(KEY_SAVED_RIFUGI, savedString)?.apply()
    }
}