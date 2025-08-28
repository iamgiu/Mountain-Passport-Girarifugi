package com.example.mountainpassport_girarifugi.data.model

data class Rifugio(
    val id: Int,
    val nome: String,
    val localita: String,
    val altitudine: Int,
    val latitudine: Double,
    val longitudine: Double,
    val immagineUrl: String? = null,
    val descrizione: String? = null,
    val tipo: TipoRifugio = TipoRifugio.RIFUGIO,
    val regione: String? = null
)

enum class TipoRifugio {
    RIFUGIO,
    BIVACCO,
    CAPANNA
}