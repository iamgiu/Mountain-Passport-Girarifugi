package com.example.mountainpassport_girarifugi.utils

import com.google.firebase.auth.FirebaseAuth

/**
 * Utility class per gestire l'utente corrente
 */
object UserManager {
    
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Ottiene l'ID dell'utente corrente autenticato
     * @return L'ID dell'utente o null se non autenticato
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
    
    /**
     * Verifica se l'utente è autenticato
     * @return true se l'utente è autenticato, false altrimenti
     */
    fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    /**
     * Ottiene l'email dell'utente corrente
     * @return L'email dell'utente o null se non autenticato
     */
    fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }
    
    /**
     * Ottiene l'ID dell'utente corrente o un ID di fallback per test
     * @return L'ID dell'utente autenticato o "user_guest" se non autenticato
     */
    fun getCurrentUserIdOrGuest(): String {
        return getCurrentUserId() ?: "user_guest"
    }
}
