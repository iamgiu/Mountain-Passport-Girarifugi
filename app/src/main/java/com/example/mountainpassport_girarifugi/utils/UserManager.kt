package com.example.mountainpassport_girarifugi.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

object UserManager {
    private const val PREFS_NAME = "mountain_passport_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_SAVED_RIFUGI = "saved_rifugi"
    private const val GUEST_USER_ID = "guest_user"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // NUOVO: Sincronizza con Firebase Auth se l'utente è loggato
        syncWithFirebaseAuth()

        // Genera un ID utente locale solo se non esiste e non è loggato con Firebase
        if (getCurrentUserId().isEmpty() && !isFirebaseUserLoggedIn()) {
            val newUserId = "user_${System.currentTimeMillis()}"
            setCurrentUserId(newUserId)
        }
    }

    /**
     * NUOVO: Sincronizza l'ID utente con Firebase Auth
     */
    private fun syncWithFirebaseAuth() {
        val firebaseUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (firebaseUserId != null) {
            // Se l'utente è loggato con Firebase, usa quell'ID
            setCurrentUserId(firebaseUserId)
        }
    }

    /**
     * NUOVO: Controlla se l'utente è loggato con Firebase
     */
    fun isFirebaseUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    /**
     * MODIFICATO: Ora considera Firebase Auth come priorità
     */
    fun getCurrentUserId(): String {
        // Prima controlla Firebase Auth
        val firebaseUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (firebaseUserId != null) {
            return firebaseUserId
        }

        // Fallback alle SharedPreferences
        return sharedPreferences?.getString(KEY_USER_ID, "") ?: ""
    }

    fun getCurrentUserIdOrGuest(): String {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) userId else GUEST_USER_ID
    }

    private fun setCurrentUserId(userId: String) {
        sharedPreferences?.edit()?.putString(KEY_USER_ID, userId)?.apply()
    }

    /**
     * NUOVO: Chiamare quando l'utente fa login con Firebase
     */
    fun onFirebaseLogin() {
        syncWithFirebaseAuth()
    }

    /**
     * MODIFICATO: Ora pulisce anche Firebase Auth
     */
    fun clearUser() {
        FirebaseAuth.getInstance().signOut()
        sharedPreferences?.edit()?.remove(KEY_USER_ID)?.apply()
    }

    // Resto dei metodi per rifugi salvati rimane uguale
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
        return !isCurrentlySaved
    }

    private fun saveSavedRifugiIds(rifugiIds: Set<Int>) {
        val savedString = rifugiIds.joinToString(",")
        sharedPreferences?.edit()?.putString(KEY_SAVED_RIFUGI, savedString)?.apply()
    }
}