package com.example.mountainpassport_girarifugi.data.repository

import android.content.Context
import android.util.Log
import com.example.mountainpassport_girarifugi.data.model.*
import com.example.mountainpassport_girarifugi.ui.notifications.NotificationsViewModel
import com.example.mountainpassport_girarifugi.utils.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.*

class PointsRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val rifugioRepository = RifugioRepository(context)
    private val activityRepository = ActivityRepository()
    private val monthlyChallengeRepository = MonthlyChallengeRepository(context)

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
        Log.d(TAG, "INIZIO recordVisit: userId=$userId, rifugioId=$rifugioId")

        return try {
            val rifugio = rifugioRepository.getRifugioById(rifugioId)
                ?: return Result.failure(Exception("Rifugio non trovato"))

            val pointsEarned = PointsCalculator.calculateVisitPoints(rifugioId, rifugio.altitudine)

            val userPoints = UserPoints(
                userId = userId,
                rifugioId = rifugioId,
                rifugioName = rifugio.nome,
                pointsEarned = pointsEarned,
                visitDate = Timestamp.now(),
                visitType = VisitType.VISIT,
                isDoublePoints = PointsCalculator.isDoublePointsRifugio(rifugioId)
            )

            val docRef = firestore.collection("user_points").add(userPoints).await()

            val firstVisit = !hasUserVisitedRifugio(userId, rifugioId)
            if (firstVisit) {
                val stampData = mapOf(
                    "refugeName" to rifugio.nome,
                    "date" to System.currentTimeMillis()
                )
                firestore.collection("users")
                    .document(userId)
                    .collection("stamps")
                    .add(stampData)
                    .await()
                Log.d(TAG, "Timbro aggiunto in users/$userId/stamps")
                
                // Notifica timbro ottenuto
                NotificationHelper.showStampObtainedNotification(context, rifugio.nome)
                NotificationsRepository().createNotification(
                    userId = userId,
                    titolo = "Timbro ottenuto!",
                    descrizione = "Hai ottenuto il timbro di ${rifugio.nome}!",
                    tipo = com.example.mountainpassport_girarifugi.ui.notifications.NotificationsViewModel.TipoNotifica.TIMBRO_OTTENUTO,
                    categoria = "rifugi",
                    rifugioId = rifugioId.toString()
                )
            }

            // Log attività per feed amici
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
            activityRepository.logUserActivity(activity)

            updateUserStats(userId, pointsEarned)

            updateRifugioStats(rifugioId)

            NotificationHelper.showPointsEarnedNotification(context, pointsEarned, rifugio.nome)
            NotificationsRepository().createPointsEarnedNotification(
                userId = userId,
                punti = pointsEarned,
                rifugioName = rifugio.nome,
                rifugioId = rifugioId
            )

            val interactionRef = firestore.collection("user_rifugio_interactions")
                .document("${userId}_${rifugioId}")

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(interactionRef)
                val currentCount = snapshot.getLong("visitCount") ?: 0
                val interaction = mapOf(
                    "userId" to userId,
                    "rifugioId" to rifugioId,
                    "isVisited" to true,
                    "visitCount" to currentCount + 1,
                    "lastInteraction" to Timestamp.now()
                )
                transaction.set(interactionRef, interaction, SetOptions.merge())
            }.await()

            monthlyChallengeRepository.checkAndNotifyChallengeCompletion(userId, pointsEarned)

            Log.d(TAG, "SUCCESS: Visita registrata completamente")
            Result.success(userPoints.copy(id = docRef.id))

        } catch (e: Exception) {
            Log.e(TAG, "EXCEPTION: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun registerRifugioVisitWithPoints(
        userId: String,
        rifugioId: Int
    ): Boolean {
        return try {
            Log.d(TAG, "Registrando visita per userId=$userId, rifugioId=$rifugioId")

            val rifugio = rifugioRepository.getRifugioById(rifugioId) ?: return false

            val pointsCalculated = PointsCalculator.calculateVisitPoints(rifugioId, rifugio.altitudine)

            val userPoints = mapOf(
                "userId" to userId,
                "rifugioId" to rifugioId,
                "rifugioName" to rifugio.nome,
                "pointsEarned" to pointsCalculated,
                "visitDate" to Timestamp.now(),
                "visitType" to "VISIT",
                "isDoublePoints" to PointsCalculator.isDoublePointsRifugio(rifugioId)
            )

            firestore.collection("user_points").add(userPoints).await()
            Log.d(TAG, "Visita salvata in user_points")

            NotificationsRepository().createPointsEarnedNotification(
                userId = userId,
                punti = pointsCalculated,
                rifugioName = rifugio.nome,
                rifugioId = rifugioId
            )
            Log.d(TAG, "Notifica punti guadagnati creata")

            if (!hasUserVisitedRifugio(userId, rifugioId)) {
                val stampData = mapOf(
                    "refugeName" to rifugio.nome,
                    "date" to System.currentTimeMillis()
                )
                firestore.collection("users")
                    .document(userId)
                    .collection("stamps")
                    .add(stampData)
                    .await()
                Log.d(TAG, "Timbro aggiunto in users/$userId/stamps")

                NotificationHelper.showStampObtainedNotification(context, rifugio.nome)
                NotificationsRepository().createNotification(
                    userId = userId,
                    titolo = "Timbro ottenuto!",
                    descrizione = "Hai ottenuto il timbro di ${rifugio.nome}!",
                    tipo = NotificationsViewModel.TipoNotifica.TIMBRO_OTTENUTO,
                    categoria = "rifugi",
                    rifugioId = rifugioId.toString()
                )
            }

            val interaction = mapOf(
                "userId" to userId,
                "rifugioId" to rifugioId,
                "isVisited" to true,
                "lastInteraction" to Timestamp.now(),
                "visitCount" to com.google.firebase.firestore.FieldValue.increment(1)
            )
            firestore.collection("user_rifugio_interactions")
                .document("${userId}_${rifugioId}")
                .set(interaction, SetOptions.merge())
                .await()

            updateUserStats(userId, pointsCalculated)
            updateRifugioStats(rifugioId)

            val activity = UserActivity(
                type = ActivityType.RIFUGIO_VISITATO,
                title = "Ha visitato ${rifugio.nome}",
                description = "Rifugio visitato in ${rifugio.localita}",
                rifugioId = rifugioId.toString(),
                rifugioName = rifugio.nome,
                rifugioLocation = rifugio.localita,
                rifugioAltitude = rifugio.altitudine.toString(),
                pointsEarned = pointsCalculated
            )
            activityRepository.logUserActivity(activity)

            NotificationHelper.showPointsEarnedNotification(context, pointsCalculated, rifugio.nome)

            monthlyChallengeRepository.checkAndNotifyChallengeCompletion(userId, pointsCalculated)

            Log.d(TAG, "Visita registrata con successo!")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Errore in registerRifugioVisitWithPoints: ${e.message}", e)
            false
        }
    }

    /**
     * Cronologia visite utente (ricarica i punti reali col PointsCalculator)
     */
    suspend fun getUserVisits(userId: String, limit: Int = 50): List<UserPoints> {
        return try {
            val snapshot = firestore.collection("user_rifugio_interactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isVisited", true)
                .limit(limit.toLong())
                .get()
                .await()

            val visits = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val rifugioId = (data["rifugioId"] as? Long)?.toInt() ?: return@mapNotNull null
                val rifugio = rifugioRepository.getRifugioById(rifugioId) ?: return@mapNotNull null

                UserPoints(
                    id = doc.id,
                    userId = data["userId"] as? String ?: "",
                    rifugioId = rifugioId,
                    rifugioName = rifugio.nome,
                    pointsEarned = PointsCalculator.calculateVisitPoints(rifugioId, rifugio.altitudine),
                    visitDate = data["visitDate"] as? Timestamp ?: Timestamp.now(),
                    visitType = VisitType.VISIT,
                    isDoublePoints = PointsCalculator.isDoublePointsRifugio(rifugioId)
                )
            }

            visits.sortedByDescending { it.visitDate.seconds }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricare le visite utente: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun hasUserVisitedRifugio(userId: String, rifugioId: Int): Boolean {
        return try {
            val docId = "${userId}_${rifugioId}"
            val doc = firestore.collection("user_rifugio_interactions")
                .document(docId)
                .get()
                .await()
            doc.exists() && (doc.getBoolean("isVisited") == true)
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel verificare visita: ${e.message}")
            false
        }
    }

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

    fun getRifugioPoints(rifugioId: Int, altitude: Int): RifugioPoints {
        return PointsCalculator.calculateTotalPoints(rifugioId, altitude)
    }

    private suspend fun updateUserStats(userId: String, pointsEarned: Int) {
        try {
            val now = Timestamp.now()
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            val currentStats = getUserPointsStats(userId) ?: UserPointsStats(userId = userId)

            val visitCalendar = Calendar.getInstance().apply { time = now.toDate() }
            val isCurrentMonth = visitCalendar.get(Calendar.MONTH) == currentMonth &&
                    visitCalendar.get(Calendar.YEAR) == currentYear

            val updatedStats = currentStats.copy(
                totalPoints = currentStats.totalPoints + pointsEarned,
                totalVisits = currentStats.totalVisits + 1,
                monthlyPoints = if (isCurrentMonth) currentStats.monthlyPoints + pointsEarned else pointsEarned,
                monthlyVisits = if (isCurrentMonth) currentStats.monthlyVisits + 1 else 1,
                lastUpdated = now
            )

            firestore.collection("user_points_stats")
                .document(userId)
                .set(updatedStats)
                .await()

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

    private suspend fun updateRifugioStats(rifugioId: Int) {
        try {
            val docRef = firestore.collection("rifugio_stats")
                .document(rifugioId.toString())

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentVisits = snapshot.getLong("totalVisits") ?: 0
                transaction.set(docRef, mapOf("totalVisits" to currentVisits + 1), SetOptions.merge())
            }.await()

            Log.d(TAG, "Statistiche rifugio aggiornate per ID: $rifugioId")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiornare le statistiche rifugio: ${e.message}")
        }
    }
}
