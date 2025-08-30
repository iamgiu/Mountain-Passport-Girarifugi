package com.example.mountainpassport_girarifugi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.mountainpassport_girarifugi.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ActivityRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.ITALIAN)

    // Cache per i nomi utente per ridurre le chiamate a Firebase
    private val userNameCache = mutableMapOf<String, String>()

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
     * VERSIONE MIGLIORATA: Ottiene il feed delle attività degli amici
     */
// Sostituisci il metodo getFriendsFeed in ActivityRepository.kt con questa versione con debug:

    suspend fun getFriendsFeed(limit: Int = 20): FeedResult {
        return try {
            val currentUserId = UserManager.getCurrentUserIdOrGuest()
            android.util.Log.d("ActivityRepository", "=== getFriendsFeed START ===")
            android.util.Log.d("ActivityRepository", "Current User ID: '$currentUserId'")

            // Verifica anche FirebaseAuth
            val firebaseUserId = FirebaseAuth.getInstance().currentUser?.uid
            android.util.Log.d("ActivityRepository", "Firebase User ID: '$firebaseUserId'")

            // 1. Ottieni tutti gli amici in una volta sola
            val friendIds = getFriendIds(currentUserId).toMutableSet()
            friendIds.add(currentUserId)

            android.util.Log.d("ActivityRepository", "Friend IDs (incluso utente corrente): $friendIds")
            android.util.Log.d("ActivityRepository", "Friend IDs ottenuti: $friendIds (size: ${friendIds.size})")

            if (friendIds.isEmpty()) {
                android.util.Log.d("ActivityRepository", "NESSUN AMICO - returning NoFriends")
                return FeedResult.NoFriends
            }

            // 2. Pre-carica i nomi utente per tutti gli amici
            preloadUserNames(friendIds)

            // 3. Ottieni le attività con query ottimizzata
            val activities = getActivitiesForUsers(friendIds, limit)
            android.util.Log.d("ActivityRepository", "Attività ottenute: ${activities.size}")

            if (activities.isEmpty()) {
                android.util.Log.d("ActivityRepository", "NESSUNA ATTIVITÀ - returning NoActivities")
                return FeedResult.NoActivities(friendIds.size)
            }

            android.util.Log.d("ActivityRepository", "SUCCESS - returning ${activities.size} activities")
            return FeedResult.Success(activities)

        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "ERRORE in getFriendsFeed", e)
            return FeedResult.Error(e.message ?: "Errore sconosciuto")
        }
    }

    // E aggiungi anche il debug a getFriendIds:
    private suspend fun getFriendIds(currentUserId: String): Set<String> {
        val friendIds = mutableSetOf<String>()

        android.util.Log.d("ActivityRepository", "=== getFriendIds START ===")
        android.util.Log.d("ActivityRepository", "Cercando amici per user: '$currentUserId'")

        try {
            val path = "users/$currentUserId/friends"
            android.util.Log.d("ActivityRepository", "Path Firebase: $path")

            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .await()

            android.util.Log.d("ActivityRepository", "Snapshot ottenuto. Documenti: ${snapshot.documents.size}")
            android.util.Log.d("ActivityRepository", "Snapshot vuoto: ${snapshot.isEmpty}")

            snapshot.documents.forEachIndexed { index, doc ->
                android.util.Log.d("ActivityRepository", "Doc[$index] - ID: '${doc.id}', exists: ${doc.exists()}")
                android.util.Log.d("ActivityRepository", "Doc[$index] - Data: ${doc.data}")

                if (doc.exists()) {
                    friendIds.add(doc.id)
                }
            }

            android.util.Log.d("ActivityRepository", "Friend IDs finali: $friendIds")

        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "ERRORE in getFriendIds", e)
        }

        android.util.Log.d("ActivityRepository", "=== getFriendIds END - returning ${friendIds.size} IDs ===")
        return friendIds
    }


    /**
     * Pre-carica i nomi utente per ridurre le chiamate a Firebase
     */
    private suspend fun preloadUserNames(userIds: Set<String>) {
        try {
            val uncachedIds = userIds.filter { !userNameCache.containsKey(it) }

            if (uncachedIds.isEmpty()) return

            // Carica i nomi in batch (Firestore supporta max 10 elementi per whereIn)
            uncachedIds.chunked(10).forEach { batch ->
                val snapshot = firestore.collection("users")
                    .whereIn("__name__", batch) // Query sui document ID
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    val userId = doc.id
                    val nome = doc.getString("nome") ?: ""
                    val cognome = doc.getString("cognome") ?: ""
                    val username = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"
                    userNameCache[userId] = username
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Error preloading usernames", e)
        }
    }

    /**
     * Ottiene le attività per un gruppo di utenti
     */
    private suspend fun getActivitiesForUsers(userIds: Set<String>, limit: Int): List<FriendActivity> {
        val activities = mutableListOf<FriendActivity>()

        try {
            // Chunking per rispettare il limite di Firestore (10 elementi per whereIn)
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

                    // Usa il nome dalla cache
                    val username = userNameCache[userId] ?: "Utente"

                    val friendActivity = FriendActivity(
                        userId = userId,
                        username = username,
                        activityType = try { ActivityType.valueOf(type) } catch (e: Exception) { ActivityType.GENERIC },
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

            // Ordina tutte le attività per timestamp e limita
            return activities.sortedByDescending { it.timestamp }.take(limit)

        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "Error getting activities", e)
            return emptyList()
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
            val username = getUserName(userId)

            activitiesSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                val type = data["type"] as? String ?: return@mapNotNull null
                val title = data["title"] as? String ?: return@mapNotNull null
                val timestamp = data["timestamp"] as? Long ?: return@mapNotNull null

                FriendActivity(
                    userId = userId,
                    username = username,
                    activityType = try { ActivityType.valueOf(type) } catch (e: Exception) { ActivityType.GENERIC },
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
     * Helper per ottenere il nome utente (con cache)
     */
    private suspend fun getUserName(userId: String): String {
        return userNameCache[userId] ?: run {
            try {
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val userData = userDoc.data
                val nome = userData?.get("nome") as? String ?: ""
                val cognome = userData?.get("cognome") as? String ?: ""
                val username = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"

                userNameCache[userId] = username
                username
            } catch (e: Exception) {
                "Utente"
            }
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

    /**
     * Pulisce la cache dei nomi utente
     */
    fun clearUserNameCache() {
        userNameCache.clear()
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
    BADGE_EARNED,
    GENERIC // Aggiunto per gestire tipi sconosciuti
}