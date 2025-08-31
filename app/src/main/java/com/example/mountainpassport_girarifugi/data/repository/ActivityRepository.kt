package com.example.mountainpassport_girarifugi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.mountainpassport_girarifugi.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import java.text.SimpleDateFormat
import java.util.*

class ActivityRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.ITALIAN)

    // Solo cache per le immagini rifugio
    private val rifugioImageCache = mutableMapOf<String, String?>()

    /**
     * Registra un'attività dell'utente (visita rifugio, achievement, etc.)
     */
    suspend fun logUserActivity(activity: UserActivity): Boolean {
        return try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                ?: UserManager.getCurrentUserIdOrGuest()

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
                "rifugioImageUrl" to activity.rifugioImageUrl,
                "pointsEarned" to activity.pointsEarned,
                "achievementType" to activity.achievementType,
                "visible" to activity.visible
            )

            firestore.collection("user_activities")
                .add(activityData)
                .await()

            true
        } catch (_: Exception) {
            false
        }
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
     * Ottiene il feed delle attività degli amici
     */
    suspend fun getFriendsFeed(limit: Int = 20): FeedResult {
        return try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                ?: UserManager.getCurrentUserIdOrGuest()
            val friendIds = getFriendIds(currentUserId).toMutableSet()
            friendIds.add(currentUserId)

            if (friendIds.size <= 1) {
                return FeedResult.NoFriends
            }

            val activities = getActivitiesForUsers(friendIds, limit)

            if (activities.isEmpty()) {
                return FeedResult.NoActivities(friendIds.size - 1)
            }

            FeedResult.Success(activities)
        } catch (e: Exception) {
            FeedResult.Error(e.message ?: "Errore sconosciuto")
        }
    }

    private suspend fun getFriendIds(currentUserId: String): Set<String> {
        return try {
            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                if (doc.exists()) doc.id else null
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Ottiene le attività per un gruppo di utenti
     */
    private suspend fun getActivitiesForUsers(userIds: Set<String>, limit: Int): List<FriendActivity> {
        val activities = mutableListOf<FriendActivity>()

        try {
            userIds.chunked(10).forEach { batch ->
                val snapshot = firestore.collection("user_activities")
                    .whereIn("userId", batch)
                    .whereEqualTo("visible", true)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach

                    val userId = data["userId"] as? String ?: return@forEach
                    val type = data["type"] as? String ?: return@forEach
                    val title = data["title"] as? String ?: return@forEach
                    val timestamp = data["timestamp"] as? Long ?: return@forEach

                    // 🔹 Carica direttamente i dati utente
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    val nome = userDoc.getString("nome") ?: ""
                    val cognome = userDoc.getString("cognome") ?: ""
                    val username = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"

                    val userAvatarUrl = userDoc.getString("profileImageUrl")
                        ?: userDoc.getString("profile_image_url")
                        ?: userDoc.getString("profileImage")
                        ?: userDoc.getString("avatar")
                        ?: userDoc.getString("profilePicture")

                    val rifugioId = data["rifugioId"] as? String
                    val rifugioImageUrl = data["rifugioImageUrl"] as? String
                        ?: getRifugioImageFromCache(rifugioId)

                    val friendActivity = FriendActivity(
                        userId = userId,
                        username = username,
                        userAvatarUrl = userAvatarUrl,
                        activityType = try { ActivityType.valueOf(type) } catch (e: Exception) { ActivityType.GENERIC },
                        title = title,
                        description = data["description"] as? String ?: "",
                        timestamp = timestamp,
                        timeAgo = getTimeAgo(timestamp),
                        rifugioId = rifugioId,
                        rifugioName = data["rifugioName"] as? String,
                        rifugioLocation = data["rifugioLocation"] as? String,
                        rifugioAltitude = data["rifugioAltitude"] as? String,
                        rifugioImageUrl = rifugioImageUrl,
                        pointsEarned = (data["pointsEarned"] as? Long)?.toInt() ?: 0,
                        achievementType = data["achievementType"] as? String
                    )

                    activities.add(friendActivity)
                }
            }

            return activities.sortedByDescending { it.timestamp }.take(limit)
        } catch (_: Exception) {
            return emptyList()
        }
    }

    /**
     * Carica l'immagine del rifugio dalla cache o da Firebase
     */
    private suspend fun getRifugioImageFromCache(rifugioId: String?): String? {
        if (rifugioId == null) return null

        return rifugioImageCache[rifugioId] ?: run {
            try {
                val rifugioImageUrl = getRifugioImageUrl(rifugioId)
                rifugioImageCache[rifugioId] = rifugioImageUrl
                rifugioImageUrl
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Carica l'URL dell'immagine del rifugio da Firebase
     */
    private suspend fun getRifugioImageUrl(rifugioId: String): String? {
        return try {
            val rifugioDoc = firestore.collection("rifugi")
                .document(rifugioId)
                .get()
                .await()

            rifugioDoc.getString("imageUrl")
                ?: rifugioDoc.getString("immagineUrl")
                ?: rifugioDoc.getString("primaryImage")
                ?: rifugioDoc.getString("image")
        } catch (_: Exception) {
            null
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

            val userDoc = firestore.collection("users").document(userId).get().await()
            val nome = userDoc.getString("nome") ?: ""
            val cognome = userDoc.getString("cognome") ?: ""
            val username = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"

            val userAvatarUrl = userDoc.getString("profileImageUrl")
                ?: userDoc.getString("profile_image_url")
                ?: userDoc.getString("profileImage")
                ?: userDoc.getString("avatar")
                ?: userDoc.getString("profilePicture")

            activitiesSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                val type = data["type"] as? String ?: return@mapNotNull null
                val title = data["title"] as? String ?: return@mapNotNull null
                val timestamp = data["timestamp"] as? Long ?: return@mapNotNull null

                val rifugioId = data["rifugioId"] as? String
                val rifugioImageUrl = data["rifugioImageUrl"] as? String
                    ?: getRifugioImageFromCache(rifugioId)

                FriendActivity(
                    userId = userId,
                    username = username,
                    userAvatarUrl = userAvatarUrl,
                    activityType = try { ActivityType.valueOf(type) } catch (e: Exception) { ActivityType.GENERIC },
                    title = title,
                    description = data["description"] as? String ?: "",
                    timestamp = timestamp,
                    timeAgo = getTimeAgo(timestamp),
                    rifugioId = rifugioId,
                    rifugioName = data["rifugioName"] as? String,
                    rifugioLocation = data["rifugioLocation"] as? String,
                    rifugioAltitude = data["rifugioAltitude"] as? String,
                    rifugioImageUrl = rifugioImageUrl,
                    pointsEarned = (data["pointsEarned"] as? Long)?.toInt() ?: 0,
                    achievementType = data["achievementType"] as? String
                )
            }
        } catch (_: Exception) {
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

    fun clearCache() {
        rifugioImageCache.clear()
    }
}

/**
 * Sealed class per rappresentare i diversi stati del feed
 */
sealed class FeedResult {
    object NoFriends : FeedResult()
    data class NoActivities(val friendsCount: Int) : FeedResult()
    data class Success(val activities: List<FriendActivity>) : FeedResult()
    data class Error(val message: String) : FeedResult()
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
    val rifugioImageUrl: String? = null,
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
    val userAvatarUrl: String? = null,
    val activityType: ActivityType,
    val title: String,
    val description: String,
    val timestamp: Long,
    val timeAgo: String,
    val rifugioId: String? = null,
    val rifugioName: String? = null,
    val rifugioLocation: String? = null,
    val rifugioAltitude: String? = null,
    val rifugioImageUrl: String? = null,
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
    BADGE_EARNED,
    GENERIC
}
