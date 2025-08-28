package com.example.mountainpassport_girarifugi.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Sistema di punti per i rifugi
 */
data class RifugioPoints(
    val rifugioId: Int = 0,
    val basePoints: Int = 0,           // Punti base calcolati dall'altitudine
    val isDoublePoints: Boolean = false, // Se il rifugio vale il doppio
    val totalPoints: Int = 0,          // Punti totali (base * 2 se double)
    val reason: String = ""            // Motivo dei punti doppi (es. "Rifugio storico")
)

/**
 * Record di punti guadagnati da un utente
 */
data class UserPoints(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val rifugioId: Int = 0,
    val rifugioName: String = "",
    val pointsEarned: Int = 0,
    val visitDate: Timestamp = Timestamp.now(),
    val visitType: VisitType = VisitType.VISIT,
    val isDoublePoints: Boolean = false
)

/**
 * Statistiche punti dell'utente
 */
data class UserPointsStats(
    val userId: String = "",
    val totalPoints: Int = 0,
    val totalVisits: Int = 0,
    val monthlyPoints: Int = 0,
    val monthlyVisits: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
)

/**
 * Tipo di visita (semplificato)
 */
enum class VisitType {
    VISIT           // Visita generica
}

/**
 * Calcolatore di punti per i rifugi
 */
object PointsCalculator {
    
    /**
     * Calcola i punti base per un rifugio basato sull'altitudine
     * Formula: 1 punto ogni 100m di altitudine (arrotondato)
     */
    fun calculateBasePoints(altitude: Int): Int {
        return (altitude / 100.0).toInt()
    }
    
    /**
     * Lista dei rifugi che valgono il doppio dei punti
     */
    private val doublePointsRifugi = setOf(
        1,   // 3A 14998 - Rifugio storico
        147, // Argentière - Rifugio storico
        150, // Arnspitzhütte - Rifugio storico

    )
    
    /**
     * Verifica se un rifugio vale il doppio dei punti
     */
    fun isDoublePointsRifugio(rifugioId: Int): Boolean {
        return doublePointsRifugi.contains(rifugioId)
    }
    
    /**
     * Calcola i punti totali per un rifugio
     */
    fun calculateTotalPoints(rifugioId: Int, altitude: Int): RifugioPoints {
        val basePoints = calculateBasePoints(altitude)
        val isDouble = isDoublePointsRifugio(rifugioId)
        val totalPoints = if (isDouble) basePoints * 2 else basePoints
        
        val reason = when {
            isDouble -> "Rifugio storico/speciale - Punti doppi!"
            altitude >= 3000 -> "Alta quota - Sfida estrema"
            altitude >= 2500 -> "Quota elevata - Impresa notevole"
            else -> "Visita standard"
        }
        
        return RifugioPoints(
            rifugioId = rifugioId,
            basePoints = basePoints,
            isDoublePoints = isDouble,
            totalPoints = totalPoints,
            reason = reason
        )
    }
    
    /**
     * Calcola punti totali per una visita (semplificato)
     */
    fun calculateVisitPoints(rifugioId: Int, altitude: Int): Int {
        val rifugioPoints = calculateTotalPoints(rifugioId, altitude)
        return rifugioPoints.totalPoints
    }
}
