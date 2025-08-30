package com.example.mountainpassport_girarifugi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.mountainpassport_girarifugi.utils.UserManager
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ActivityRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.ITALIAN)

    /**
     * Registra un'attività dell'utente (visita rifugio, achievement, etc.)
     */
    suspend fun logUserActivity(activity: UserActivity): Boolean {
        return try {
            val currentUserId = UserManager.getCurrentUserIdOrGuest()

            val activityData = mapOf(
                "userId" to currentUserId,
                "type" to activity.type.name,
                "title" to activity.title,
                "description" to activity.description,
                "timestamp" to activity.timestamp,
                "rifugioId" to activity.rifugioId,
                "rifugioName" to activity.rifugioName,
                "rifugioLocation" to activity.rifugioLocation,
                "rifugioAltitude" to activity.rifugioAltitude,
                "pointsEarned" to activity.pointsEarned,
                "achievementType" to activity.achievementType,
                "visible" to activity.visible
            )

            firestore.collection("user_activities")
                .add(activityData)
                .await()

            true
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Error logging activity", e)
            false
        }
    }

    /**
     * Registra specificamente la visita a un rifugio
     */
    suspend fun logRifugioVisit(
        rifugioId: String,
        rifugioName: String,
        rifugioLocation: String,
        rifugioAltitude: String,
        pointsEarned: Int = 50
    ): Boolean {
        val activity = UserActivity(
            type = ActivityType.RIFUGIO_VISITATO,
            title = "Ha visitato $rifugioName",
            description = "Rifugio visitato in $rifugioLocation",
            rifugioId = rifugioId,
            rifugioName = rifugioName,
            rifugioLocation = rifugioLocation,
            rifugioAltitude = rifugioAltitude,
            pointsEarned = pointsEarned
        )

        return logUserActivity(activity)
    }

    /**
     * Registra il raggiungimento di un achievement
     */
    suspend fun logAchievement(
        achievementType: String,
        title: String,
        description: String,
        pointsEarned: Int = 100
    ): Boolean {
        val activity = UserActivity(
            type = ActivityType.ACHIEVEMENT,
            title = title,
            description = description,
            achievementType = achievementType,
            pointsEarned = pointsEarned
        )

        return logUserActivity(activity)
    }

    /**
     * Ottiene il feed delle attività degli amici dell'utente corrente
     */
    suspend fun getFriendsFeed(limit: Int = 20): List<FriendActivity> {
        return try {
            val currentUserId = UserManager.getCurrentUserIdOrGuest()

            // Prima ottieni la lista degli amici
            val friendsSnapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .await()

            val friendIds = friendsSnapshot.documents.map { it.id }

            if (friendIds.isEmpty()) {
                return emptyList()
            }

            // Ottieni le attività degli amici (in batch per evitare limiti di Firestore)
            val activities = mutableListOf<FriendActivity>()

            // Firestore ha un limite di 10 elementi per query "in", quindi dividiamo in batch
            friendIds.chunked(10).forEach { batch ->
                val batchActivities = firestore.collection("user_activities")
                    .whereIn("userId", batch)
                    .whereEqualTo("visible", true)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                for (doc in batchActivities.documents) {
                    val data = doc.data ?: continue

                    val userId = data["userId"] as? String ?: continue
                    val type = data["type"] as? String ?: continue
                    val title = data["title"] as? String ?: continue
                    val timestamp = data["timestamp"] as? Long ?: continue

                    // Ottieni i dettagli dell'utente
                    val userDoc = firestore.collection("users")
                        .document(userId)
                        .get()
                        .await()

                    val userData = userDoc.data
                    val nome = userData?.get("nome") as? String ?: ""
                    val cognome = userData?.get("cognome") as? String ?: ""
                    val username = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"

                    val friendActivity = FriendActivity(
                        userId = userId,
                        username = username,
                        activityType = ActivityType.valueOf(type),
                        title = title,
                        description = data["description"] as? String ?: "",
                        timestamp = timestamp,
                        timeAgo = getTimeAgo(timestamp),
                        rifugioId = data["rifugioId"] as? String,
                        rifugioName = data["rifugioName"] as? String,
                        rifugioLocation = data["rifugioLocation"] as? String,
                        rifugioAltitude = data["rifugioAltitude"] as? String,
                        pointsEarned = (data["pointsEarned"] as? Long)?.toInt() ?: 0,
                        achievementType = data["achievementType"] as? String
                    )

                    activities.add(friendActivity)
                }
            }

            // Ordina per timestamp e prendi solo i primi `limit` elementi
            activities.sortedByDescending { it.timestamp }.take(limit)

        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Error fetching friends feed", e)
            emptyList()
        }
    }

    /**
     * Ottiene le attività di un utente specifico
     */
    suspend fun getUserActivities(userId: String, limit: Int = 10): List<FriendActivity> {
        return try {
            val activitiesSnapshot = firestore.collection("user_activities")
                .whereEqualTo("userId", userId)
                .whereEqualTo("visible", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            // Ottieni i dettagli dell'utente una volta sola
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val userData = userDoc.data
            val nome = userData?.get("nome") as? String ?: ""
            val cognome = userData?.get("cognome") as? String ?: ""
            val username = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"

            activitiesSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                val type = data["type"] as? String ?: return@mapNotNull null
                val title = data["title"] as? String ?: return@mapNotNull null
                val timestamp = data["timestamp"] as? Long ?: return@mapNotNull null

                FriendActivity(
                    userId = userId,
                    username = username,
                    activityType = ActivityType.valueOf(type),
                    title = title,
                    description = data["description"] as? String ?: "",
                    timestamp = timestamp,
                    timeAgo = getTimeAgo(timestamp),
                    rifugioId = data["rifugioId"] as? String,
                    rifugioName = data["rifugioName"] as? String,
                    rifugioLocation = data["rifugioLocation"] as? String,
                    rifugioAltitude = data["rifugioAltitude"] as? String,
                    pointsEarned = (data["pointsEarned"] as? Long)?.toInt() ?: 0,
                    achievementType = data["achievementType"] as? String
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Error fetching user activities", e)
            emptyList()
        }
    }

    /**
     * Calcola il tempo trascorso in formato leggibile
     */
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Adesso"
            diff < 3600000 -> "${diff / 60000} minuti fa"
            diff < 86400000 -> "${diff / 3600000} ore fa"
            diff < 604800000 -> "${diff / 86400000} giorni fa"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date(timestamp))
        }
    }
}

/**
 * Data class per rappresentare un'attività dell'utente
 */
data class UserActivity(
    val type: ActivityType,
    val title: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rifugioId: String? = null,
    val rifugioName: String? = null,
    val rifugioLocation: String? = null,
    val rifugioAltitude: String? = null,
    val pointsEarned: Int = 0,
    val achievementType: String? = null,
    val visible: Boolean = true
)

/**
 * Data class per rappresentare un'attività di un amico nel feed
 */
data class FriendActivity(
    val userId: String,
    val username: String,
    val activityType: ActivityType,
    val title: String,
    val description: String,
    val timestamp: Long,
    val timeAgo: String,
    val rifugioId: String? = null,
    val rifugioName: String? = null,
    val rifugioLocation: String? = null,
    val rifugioAltitude: String? = null,
    val pointsEarned: Int = 0,
    val achievementType: String? = null
)

/**
 * Enum per i tipi di attività
 */
enum class ActivityType {
    RIFUGIO_VISITATO,
    ACHIEVEMENT,
    PUNTI_GUADAGNATI,
    RECENSIONE,
    MILESTONE,
    BADGE_EARNED
}