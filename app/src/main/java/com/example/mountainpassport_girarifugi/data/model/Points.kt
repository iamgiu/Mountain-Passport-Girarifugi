package com.example.mountainpassport_girarifugi.data.model

import com.example.mountainpassport_girarifugi.data.repository.MonthlyChallengeRepository
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

    // Repository per le sfide mensili
    private val monthlyChallengeRepository = MonthlyChallengeRepository()

    /**
     * Calcola i punti totali per la visita a un rifugio
     */
    suspend fun calculateVisitPoints(rifugioId: Int, altitudine: Int): Int {
        val basePoints = calculateBasePoints(altitudine)
        val isBonus = isCurrentlyBonusRifugio(rifugioId)

        return if (isBonus) {
            android.util.Log.d("PointsCalculator", "🎯 Rifugio $rifugioId è BONUS! Punti raddoppiati: $basePoints -> ${basePoints * 2}")
            basePoints * 2
        } else {
            android.util.Log.d("PointsCalculator", "📍 Rifugio $rifugioId punti normali: $basePoints")
            basePoints
        }
    }

    /**
     * Calcola i punti base in base all'altitudine
     */
    private fun calculateBasePoints(altitudine: Int): Int {
        return when (altitudine) {
            in 0..500 -> 10
            in 501..1000 -> 15
            in 1001..1500 -> 20
            in 1501..2000 -> 25
            in 2001..2500 -> 30
            in 2501..3000 -> 35
            in 3001..3500 -> 40
            in 3501..4000 -> 45
            in 4001..4500 -> 50
            else -> 55
        }
    }

    /**
     * Verifica se un rifugio è attualmente nella lista dei bonus
     */
    suspend fun isCurrentlyBonusRifugio(rifugioId: Int): Boolean {
        return try {
            val challenge = monthlyChallengeRepository.getCurrentChallenge()
            val isBonus = challenge?.bonusRifugi?.contains(rifugioId) ?: false
            android.util.Log.d("PointsCalculator", "Verifica bonus per rifugio $rifugioId: $isBonus")
            isBonus
        } catch (e: Exception) {
            android.util.Log.e("PointsCalculator", "Errore verifica bonus: ${e.message}")
            false
        }
    }

    /**
     * Metodo legacy per compatibilità
     */
    @Deprecated("Use calculateVisitPoints instead")
    fun isDoublePointsRifugio(rifugioId: Int): Boolean {
        // Questo metodo è deprecato, ma mantenuto per compatibilità
        // I punti doppi ora sono gestiti dinamicamente tramite le sfide mensili
        return false
    }

    /**
     * Calcola i punti totali per visualizzazione UI
     */
    fun calculateTotalPoints(rifugioId: Int, altitude: Int): RifugioPoints {
        val basePoints = calculateBasePoints(altitude)

        return RifugioPoints(
            rifugioId = rifugioId,
            basePoints = basePoints,
            isDoublePoints = false,
            totalPoints = basePoints,
            reason = ""
        )
    }

    /**
     * Ottiene i punti bonus potenziali per un rifugio
     */
    suspend fun getBonusPointsForRifugio(rifugioId: Int, altitudine: Int): Int? {
        return if (isCurrentlyBonusRifugio(rifugioId)) {
            calculateBasePoints(altitudine) // I punti bonus sono uguali ai punti base
        } else null
    }

    /**
     * Verifica se è attiva una sfida mensile
     */
    suspend fun hasActiveMonthlyChallenge(): Boolean {
        return try {
            val challenge = monthlyChallengeRepository.getCurrentChallenge()
            challenge?.bonusRifugi?.isNotEmpty() ?: false
        } catch (e: Exception) {
            false
        }
    }
}
