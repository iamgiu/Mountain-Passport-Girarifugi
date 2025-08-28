package com.example.mountainpassport_girarifugi.data.repository

import android.content.Context
import android.util.Log
import com.example.mountainpassport_girarifugi.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

class PointsRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val rifugioRepository = RifugioRepository(context)
    
    companion object {
        private const val TAG = "PointsRepository"
    }
    
    /**
     * Registra una visita a un rifugio e assegna i punti
     */
    suspend fun recordVisit(
        userId: String,
        rifugioId: Int
    ): Result<UserPoints> {
        return try {
            // Ottieni i dati del rifugio
            val rifugio = rifugioRepository.getRifugioById(rifugioId)
            if (rifugio == null) {
                return Result.failure(Exception("Rifugio non trovato"))
            }
            
            // Calcola i punti
            val pointsEarned = PointsCalculator.calculateVisitPoints(
                rifugioId, 
                rifugio.altitudine
            )
            
            // Crea il record della visita
            val userPoints = UserPoints(
                userId = userId,
                rifugioId = rifugioId,
                rifugioName = rifugio.nome,
                pointsEarned = pointsEarned,
                visitDate = Timestamp.now(),
                visitType = VisitType.VISIT,
                isDoublePoints = PointsCalculator.isDoublePointsRifugio(rifugioId)
            )
            
            // Salva in Firebase
            val docRef = firestore.collection("user_points")
                .add(userPoints)
                .await()
            
            // Aggiorna le statistiche dell'utente
            updateUserStats(userId, pointsEarned)
            
            // Aggiorna le statistiche del rifugio
            updateRifugioStats(rifugioId)
            
            Log.d(TAG, "Visita registrata: ${userPoints.rifugioName} - ${userPoints.pointsEarned} punti")
            
            Result.success(userPoints.copy(id = docRef.id))
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel registrare la visita: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Ottieni le statistiche punti di un utente
     */
    suspend fun getUserPointsStats(userId: String): UserPointsStats? {
        return try {
            val doc = firestore.collection("user_points_stats")
                .document(userId)
                .get()
                .await()
            
            doc.toObject(UserPointsStats::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricare le statistiche utente: ${e.message}")
            null
        }
    }
    
    /**
     * Ottieni la cronologia delle visite di un utente
     */
    suspend fun getUserVisits(userId: String, limit: Int = 50): List<UserPoints> {
        return try {
            val snapshot = firestore.collection("user_points")
                .whereEqualTo("userId", userId)
                .orderBy("visitDate", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserPoints::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricare le visite utente: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Verifica se un utente ha già visitato un rifugio
     */
    suspend fun hasUserVisitedRifugio(userId: String, rifugioId: Int): Boolean {
        return try {
            val snapshot = firestore.collection("user_points")
                .whereEqualTo("userId", userId)
                .whereEqualTo("rifugioId", rifugioId)
                .limit(1)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel verificare la visita: ${e.message}")
            false
        }
    }
    
    /**
     * Ottieni i punti disponibili per un rifugio
     */
    fun getRifugioPoints(rifugioId: Int, altitude: Int): RifugioPoints {
        return PointsCalculator.calculateTotalPoints(rifugioId, altitude)
    }
    
    /**
     * Aggiorna le statistiche dell'utente
     */
    private suspend fun updateUserStats(userId: String, pointsEarned: Int) {
        try {
            val now = Timestamp.now()
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Ottieni le statistiche attuali
            val currentStats = getUserPointsStats(userId) ?: UserPointsStats(userId = userId)
            
            // Calcola il mese corrente
            val visitCalendar = Calendar.getInstance()
            visitCalendar.time = now.toDate()
            val isCurrentMonth = visitCalendar.get(Calendar.MONTH) == currentMonth && 
                               visitCalendar.get(Calendar.YEAR) == currentYear
            
            // Aggiorna le statistiche
            val updatedStats = currentStats.copy(
                totalPoints = currentStats.totalPoints + pointsEarned,
                totalVisits = currentStats.totalVisits + 1,
                monthlyPoints = if (isCurrentMonth) currentStats.monthlyPoints + pointsEarned else pointsEarned,
                monthlyVisits = if (isCurrentMonth) currentStats.monthlyVisits + 1 else 1,
                lastUpdated = now
            )
            
            // Salva in Firebase
            firestore.collection("user_points_stats")
                .document(userId)
                .set(updatedStats)
                .await()
                
            Log.d(TAG, "Statistiche utente aggiornate: ${updatedStats.totalPoints} punti totali")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiornare le statistiche utente: ${e.message}")
        }
    }
    
    /**
     * Aggiorna le statistiche del rifugio
     */
    private suspend fun updateRifugioStats(rifugioId: Int) {
        try {
            val docRef = firestore.collection("rifugio_stats")
                .document(rifugioId.toString())
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentVisits = snapshot.getLong("totalVisits") ?: 0
                
                transaction.update(docRef, "totalVisits", currentVisits + 1)
            }.await()
            
            Log.d(TAG, "Statistiche rifugio aggiornate per ID: $rifugioId")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiornare le statistiche rifugio: ${e.message}")
        }
    }
    
    /**
     * Ottieni la classifica degli utenti per punti
     */
    suspend fun getLeaderboard(limit: Int = 100): List<UserPointsStats> {
        return try {
            val snapshot = firestore.collection("user_points_stats")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserPointsStats::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricare la classifica: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Ottieni la classifica mensile
     */
    suspend fun getMonthlyLeaderboard(limit: Int = 100): List<UserPointsStats> {
        return try {
            val snapshot = firestore.collection("user_points_stats")
                .orderBy("monthlyPoints", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserPointsStats::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricare la classifica mensile: ${e.message}")
            emptyList()
        }
    }
}
