package com.example.mountainpassport_girarifugi.data.repository

import android.content.Context
import android.util.Log
import com.example.mountainpassport_girarifugi.data.model.*
import com.example.mountainpassport_girarifugi.utils.NotificationHelper
import com.example.mountainpassport_girarifugi.data.repository.NotificationsRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.*

class PointsRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val rifugioRepository = RifugioRepository(context)
    private val activityRepository = ActivityRepository()
    
    companion object {
        private const val TAG = "PointsRepository"
    }

    /**
     * Registra una visita a un rifugio e assegna i punti - VERSIONE AGGIORNATA
     */
    suspend fun recordVisit(
        userId: String,
        rifugioId: Int
    ): Result<UserPoints> {
        android.util.Log.d("PointsRepository", "🚀 INIZIO recordVisit: userId=$userId, rifugioId=$rifugioId")

        return try {
            // Verifica doppia visita
            if (hasUserVisitedRifugio(userId, rifugioId)) {
                android.util.Log.w("PointsRepository", "⚠️ STOP: Rifugio già visitato")
                return Result.failure(Exception("Hai già visitato questo rifugio"))
            }

            // Ottieni rifugio
            android.util.Log.d("PointsRepository", "🏔️ LOAD: Caricando dati rifugio")
            val rifugio = rifugioRepository.getRifugioById(rifugioId)
            if (rifugio == null) {
                android.util.Log.e("PointsRepository", "❌ ERROR: Rifugio non trovato")
                return Result.failure(Exception("Rifugio non trovato"))
            }

            android.util.Log.d("PointsRepository", "✅ RIFUGIO: ${rifugio.nome}, altitudine=${rifugio.altitudine}")

            // Calcola punti
            val pointsEarned = PointsCalculator.calculateVisitPoints(rifugioId, rifugio.altitudine)
            android.util.Log.d("PointsRepository", "🧮 CALC: Punti calcolati = $pointsEarned")

            // Crea UserPoints
            val userPoints = UserPoints(
                userId = userId,
                rifugioId = rifugioId,
                rifugioName = rifugio.nome,
                pointsEarned = pointsEarned,
                visitDate = Timestamp.now(),
                visitType = VisitType.VISIT,
                isDoublePoints = PointsCalculator.isDoublePointsRifugio(rifugioId)
            )
            android.util.Log.d("PointsRepository", "📦 CREATED: UserPoints object")

            // Salva in Firebase
            android.util.Log.d("PointsRepository", "💾 SAVE: Salvando in user_points collection")
            val docRef = firestore.collection("user_points").add(userPoints).await()
            android.util.Log.d("PointsRepository", "✅ SAVED: Document ID = ${docRef.id}")

            // Log attività per feed amici
            android.util.Log.d("PointsRepository", "📝 ACTIVITY: Logging user activity")
            try {
                val activityRepository = ActivityRepository()
                val activity = UserActivity(
                    type = ActivityType.RIFUGIO_VISITATO,
                    title = "Ha visitato ${rifugio.nome}",
                    description = "Rifugio visitato in ${rifugio.localita}",
                    rifugioId = rifugioId.toString(),
                    rifugioName = rifugio.nome,
                    rifugioLocation = rifugio.localita,
                    rifugioAltitude = rifugio.altitudine.toString(),
                    pointsEarned = pointsEarned
                )

                val activityLogged = activityRepository.logUserActivity(activity)
                android.util.Log.d("PointsRepository", "📝 ACTIVITY: Logged = $activityLogged")
            } catch (e: Exception) {
                android.util.Log.e("PointsRepository", "❌ ACTIVITY ERROR: ${e.message}")
            }

            // Aggiorna stats utente
            android.util.Log.d("PointsRepository", "📊 STATS: Aggiornando statistiche utente")
            updateUserStats(userId, pointsEarned)

            // Aggiorna stats rifugio
            android.util.Log.d("PointsRepository", "🏔️ STATS: Aggiornando statistiche rifugio")
            updateRifugioStats(rifugioId)

            // Notifiche
            android.util.Log.d("PointsRepository", "🔔 NOTIF: Inviando notifiche")
            try {
                NotificationHelper.showPointsEarnedNotification(context, pointsEarned, rifugio.nome)

                val notificationsRepository = NotificationsRepository()
                notificationsRepository.createPointsEarnedNotification(
                    userId = userId,
                    punti = pointsEarned,
                    rifugioName = rifugio.nome,
                    rifugioId = rifugioId
                )
                android.util.Log.d("PointsRepository", "✅ NOTIF: Notifiche inviate")
            } catch (e: Exception) {
                android.util.Log.e("PointsRepository", "❌ NOTIF ERROR: ${e.message}")
            }

            android.util.Log.d("PointsRepository", "🎉 SUCCESS: Visita registrata completamente")
            Result.success(userPoints.copy(id = docRef.id))

        } catch (e: Exception) {
            android.util.Log.e("PointsRepository", "💥 EXCEPTION: ${e.message}", e)
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

            val currentStats = getUserPointsStats(userId) ?: UserPointsStats(userId = userId)

            val visitCalendar = Calendar.getInstance()
            visitCalendar.time = now.toDate()
            val isCurrentMonth = visitCalendar.get(Calendar.MONTH) == currentMonth &&
                    visitCalendar.get(Calendar.YEAR) == currentYear

            val updatedStats = currentStats.copy(
                totalPoints = currentStats.totalPoints + pointsEarned,
                totalVisits = currentStats.totalVisits + 1,
                monthlyPoints = if (isCurrentMonth) currentStats.monthlyPoints + pointsEarned else pointsEarned,
                monthlyVisits = if (isCurrentMonth) currentStats.monthlyVisits + 1 else 1,
                lastUpdated = now
            )

            // Aggiorna user_points_stats (leaderboard)
            firestore.collection("user_points_stats")
                .document(userId)
                .set(updatedStats)
                .await()

            // Aggiorna anche users/collection se vuoi mantenere sincronizzati i punti
            val userDoc = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userDoc)
                val currentPoints = userSnapshot.getLong("points") ?: 0
                val currentRefuges = userSnapshot.getLong("refugesCount") ?: 0

                transaction.update(userDoc, mapOf(
                    "points" to currentPoints + pointsEarned,
                    "refugesCount" to currentRefuges + 1
                ))
            }.await()

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
                val newVisits = currentVisits + 1

                // invece di transaction.update → uso set + merge
                transaction.set(docRef, mapOf("totalVisits" to newVisits), SetOptions.merge())
            }.await()

            Log.d(TAG, "Statistiche rifugio aggiornate per ID: $rifugioId")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiornare le statistiche rifugio: ${e.message}")
        }
    }
    
    /**
     * Verifica se l'utente ha già visitato un rifugio
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
            Log.e(TAG, "Errore nel verificare se l'utente ha visitato il rifugio: ${e.message}")
            false
        }
    }
}
