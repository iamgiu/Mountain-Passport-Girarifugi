package com.example.mountainpassport_girarifugi.user

data class User(
    val uid: String = "",
    val email: String = "",
    val nome: String = "",
    val cognome: String = "",
    val nickname: String = "",
    val profileImageUrl: String? = null, // AGGIUNGI LA VIRGOLA QUI
    val points: Int = 0,
    val refugesCount: Int = 0
) {
    // Aggiorna anche il costruttore vuoto per includere i nuovi campi
    constructor() : this("", "", "", "", "", null, 0, 0)

    fun isProfileComplete(): Boolean {
        return nome.isNotBlank() && cognome.isNotBlank() && nickname.isNotBlank()
    }
}