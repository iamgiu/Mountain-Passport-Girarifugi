package com.example.mountainpassport_girarifugi.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository per gestire l'invio di notifiche push
 * Nota: Per l'invio effettivo di notifiche push, dovrai implementare
 * Firebase Cloud Functions o utilizzare l'API di Firebase Cloud Messaging
 */
class PushNotificationRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "PushNotificationRepo"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_NOTIFICATIONS = "notifications"
    }

    /**
     * Salva il token FCM dell'utente nel database
     */
    suspend fun saveFCMToken(userId: String, token: String): Boolean {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update("fcmToken", token)
                .await()
            Log.d(TAG, "Token FCM salvato per l'utente: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel salvare il token FCM: ${e.message}")
            false
        }
    }

    /**
     * Ottiene il token FCM di un utente
     */
    suspend fun getFCMToken(userId: String): String? {
        return try {
            val document = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            document.getString("fcmToken")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel recuperare il token FCM: ${e.message}")
            null
        }
    }

    /**
     * Prepara i dati per l'invio di una notifica push
     * Nota: L'invio effettivo richiede Firebase Cloud Functions
     */
    suspend fun preparePushNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        category: String,
        rifugioId: String? = null,
        utenteId: String? = null,
        achievementId: String? = null,
        puntiGuadagnati: Int? = null
    ): Boolean {
        return try {
            // Salva la notifica nel database per l'invio
            val notificationData = mapOf(
                "userId" to userId,
                "title" to title,
                "message" to message,
                "type" to type,
                "category" to category,
                "rifugioId" to rifugioId,
                "utenteId" to utenteId,
                "achievementId" to achievementId,
                "puntiGuadagnati" to puntiGuadagnati,
                "timestamp" to System.currentTimeMillis(),
                "sent" to false
            )

            firestore.collection("pushNotifications")
                .add(notificationData)
                .await()

            Log.d(TAG, "Notifica push preparata per l'utente: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella preparazione della notifica push: ${e.message}")
            false
        }
    }

    /**
     * Invia notifica push per richiesta di amicizia
     */
    suspend fun sendFriendRequestPushNotification(
        receiverId: String,
        senderName: String
    ): Boolean {
        return preparePushNotification(
            userId = receiverId,
            title = senderName,
            message = "Richiesta di amicizia",
            type = "RICHIESTA_AMICIZIA",
            category = "amici",
            utenteId = auth.currentUser?.uid
        )
    }

    /**
     * Invia notifica push per amicizia accettata
     */
    suspend fun sendFriendAcceptedPushNotification(
        receiverId: String,
        accepterName: String
    ): Boolean {
        return preparePushNotification(
            userId = receiverId,
            title = "Richiesta accettata",
            message = "$accepterName ha accettato la tua richiesta di amicizia",
            type = "RICHIESTA_AMICIZIA",
            category = "amici",
            utenteId = auth.currentUser?.uid
        )
    }

    /**
     * Invia notifica push per punti guadagnati
     */
    suspend fun sendPointsEarnedPushNotification(
        userId: String,
        points: Int,
        rifugioName: String? = null
    ): Boolean {
        val message = if (rifugioName != null) {
            "Hai guadagnato $points punti visitando $rifugioName!"
        } else {
            "Hai guadagnato $points punti!"
        }

        return preparePushNotification(
            userId = userId,
            title = "Punti guadagnati",
            message = message,
            type = "PUNTI_OTTENUTI",
            category = "punti",
            puntiGuadagnati = points
        )
    }

    /**
     * Invia notifica push per nuovo rifugio
     */
    suspend fun sendNewRifugioPushNotification(
        userId: String,
        rifugioName: String,
        rifugioId: String
    ): Boolean {
        return preparePushNotification(
            userId = userId,
            title = "Nuovo rifugio disponibile",
            message = "$rifugioName è ora disponibile per la visita!",
            type = "NUOVO_MEMBRO_GRUPPO",
            category = "rifugi",
            rifugioId = rifugioId
        )
    }

    /**
     * Invia notifica push per achievement sbloccato
     */
    suspend fun sendAchievementPushNotification(
        userId: String,
        achievementName: String,
        achievementId: String
    ): Boolean {
        return preparePushNotification(
            userId = userId,
            title = "Achievement sbloccato!",
            message = "Hai sbloccato: $achievementName",
            type = "SFIDA_COMPLETATA",
            category = "achievements",
            achievementId = achievementId
        )
    }

    /**
     * Rimuove il token FCM quando l'utente effettua il logout
     */
    suspend fun removeFCMToken(userId: String): Boolean {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update("fcmToken", null)
                .await()
            Log.d(TAG, "Token FCM rimosso per l'utente: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella rimozione del token FCM: ${e.message}")
            false
        }
    }
}
