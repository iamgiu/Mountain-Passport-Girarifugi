package com.example.mountainpassport_girarifugi.user

data class User(
    val uid: String = "",
    val email: String = "",
    val nome: String = "",
    val cognome: String = "",
    val nickname: String = "",
    val profileImageUrl: String = "" // String perché usero l'URL
) {
    // Firestore requires a no-argument constructor
    constructor() : this("", "", "", "", "")

    fun isProfileComplete(): Boolean {
        return nome.isNotBlank() && cognome.isNotBlank() && nickname.isNotBlank()
    }
}