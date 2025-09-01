package com.example.mountainpassport_girarifugi.data.repository

import com.example.mountainpassport_girarifugi.ui.notifications.NotificationsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NotificationsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val COLLECTION_NOTIFICATIONS = "notifications"
    }

    /**
     *
     * Prende tutte le notifiche dell'utente corrente
     *
     * Controlla che ci sia un utente loggato
     *
     * Prende tutte le notifiche su Firebase li converte in FirebaseNotifica (dataclass)
     *
     * Li trasforma in NotificationsViewModel.Notifica
     *
     * Ordina i risultati per timestamp piÃ¹ recente
     *
     */
    suspend fun getAllNotifications(): List<NotificationsViewModel.Notifica> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return emptyList()
        }

        return try {
            val snapshot = firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val notifiche = snapshot.documents.mapNotNull { doc ->
                try {
                    val firebaseNotifica = doc.toObject(FirebaseNotifica::class.java)?.copy(id = doc.id)
                    firebaseNotifica?.toNotifica()
                } catch (e: Exception) {
                    null
                }
            }

            notifiche.sortedByDescending { notifica ->
                parseTempoToTimestamp(notifica.tempo)
            }

        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     *
     * Sposta una notifica in "Precedenti" aggiornando il timestamp
     *
     * Calcola un timestamp di 2 giorni fa e aggiorna la notifica
     *
     */
    suspend fun moveNotificationToPrevious(notificaId: String): Boolean {
        return try {
            val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000)
            val previousTimestamp = Timestamp(
                twoDaysAgo / 1000,
                ((twoDaysAgo % 1000) * 1000000).toInt()
            )

            firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(notificaId)
                .update(
                    mapOf(
                        "timestamp" to previousTimestamp,
                        "isLetta" to true
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Conversione del campo tempo
     *
     * Trasforma la stringa tempo in millisecondi
     *
     * Serve per ordinare le notifica lato client
     *
     */
    private fun parseTempoToTimestamp(tempo: String): Long {
        val now = System.currentTimeMillis()

        return when {
            tempo == "Ora" -> now
            tempo.contains("minuti fa") -> {
                val minutes = tempo.substringBefore(" minuti fa").toLongOrNull() ?: 0
                now - (minutes * 60 * 1000)
            }
            tempo.contains("ore fa") -> {
                val hours = tempo.substringBefore(" ore fa").toLongOrNull() ?: 0
                now - (hours * 60 * 60 * 1000)
            }
            tempo.contains("giorno fa") -> {
                now - (24 * 60 * 60 * 1000)
            }
            tempo.contains("giorni fa") -> {
                val days = tempo.substringBefore(" giorni fa").toLongOrNull() ?: 0
                now - (days * 24 * 60 * 60 * 1000)
            }
            tempo.contains("mesi fa") -> {
                val months = tempo.substringBefore(" mesi fa").toLongOrNull() ?: 0
                now - (months * 30 * 24 * 60 * 60 * 1000)
            }
            else -> 0L
        }
    }

    /**
     *
     *  Observer per le notifiche in tempo reale
     *
     */
    fun observeNotifications(): Flow<List<NotificationsViewModel.Notifica>> = callbackFlow {

        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val notifiche = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FirebaseNotifica::class.java)?.copy(id = doc.id)?.toNotifica()
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                val sortedNotifiche = notifiche.sortedByDescending { notifica ->
                    parseTempoToTimestamp(notifica.tempo)
                }

                trySend(sortedNotifiche)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     *
     * Segna una notifica come letta
     *
     */
    suspend fun markAsRead(notificaId: String): Boolean {
        return try {
            firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(notificaId)
                .update("isLetta", true)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Segna tutte le notifiche come lette
     *
     */
    suspend fun markAllAsRead(): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            val batch = firestore.batch()
            val snapshot = firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isLetta", false)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isLetta", true)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Elimina una notifica
     *
     */
    suspend fun deleteNotification(notificaId: String): Boolean {
        return try {
            firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(notificaId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Crea una nuova notifica
     *
     */
    suspend fun createNotification(
        userId: String,
        titolo: String,
        descrizione: String,
        tipo: NotificationsViewModel.TipoNotifica,
        categoria: String,
        rifugioId: String? = null,
        utenteId: String? = null,
        achievementId: String? = null,
        puntiGuadagnati: Int? = null,
        senderAvatarUrl: String? = null
    ): Boolean {
        return try {
            val docRef = firestore.collection(COLLECTION_NOTIFICATIONS).document()
            val notifica = FirebaseNotifica(
                id = docRef.id,
                userId = userId,
                titolo = titolo,
                descrizione = descrizione,
                tipo = tipo.name,
                categoria = categoria,
                timestamp = Timestamp.now(),
                isLetta = false,
                rifugioId = rifugioId,
                utenteId = utenteId,
                achievementId = achievementId,
                puntiGuadagnati = puntiGuadagnati,
                avatarUrl = senderAvatarUrl
            )

            docRef.set(notifica).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Crea notifica di richiesta amicizia
     *
     */
    suspend fun createFriendRequestNotification(
        receiverId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String? = null
    ): Boolean {
        return try {

            val success = createNotification(
                userId = receiverId,
                titolo = senderName,
                descrizione = "Richiesta di amicizia",
                tipo = NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA,
                categoria = "amici",
                utenteId = senderId
            )

            if (success) {
            } else {
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Crea notifica di amicizia accettata
     *
     */
    suspend fun createFriendAcceptedNotification(
        receiverId: String,
        accepterName: String,
        accepterId: String
    ): Boolean {
        return try {

            createNotification(
                userId = receiverId,
                titolo = "Richiesta accettata",
                descrizione = "$accepterName ha accettato la tua richiesta di amicizia",
                tipo = NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA,
                categoria = "amici",
                utenteId = accepterId
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Crea notifica per punti guadagnati
     *
     */
    suspend fun createPointsEarnedNotification(
        userId: String,
        punti: Int,
        rifugioName: String?,
        rifugioId: Int? = null
    ): Boolean {
        val descrizione = if (rifugioName != null) {
            "Hai guadagnato $punti punti visitando $rifugioName!"
        } else {
            "Hai guadagnato $punti punti!"
        }

        return createNotification(
            userId = userId,
            titolo = "Punti guadagnati",
            descrizione = descrizione,
            tipo = NotificationsViewModel.TipoNotifica.PUNTI_OTTENUTI,
            categoria = "rifugi",
            puntiGuadagnati = punti,
            rifugioId = rifugioId?.toString()
        )
    }

    /**
     *
     * Elimina notifiche di richieste amicizia obsolete
     *
     */
    suspend fun cleanupObsoleteRequests(userId: String): Boolean {
        return try {

            val firestore = FirebaseFirestore.getInstance()

            val notificationsSnapshot = firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("tipo", "RICHIESTA_AMICIZIA")
                .get()
                .await()

            val batch = firestore.batch()
            var deletedCount = 0

            for (notificationDoc in notificationsSnapshot.documents) {
                val notification = notificationDoc.toObject(FirebaseNotifica::class.java)
                val senderId = notification?.utenteId

                if (!senderId.isNullOrBlank()) {
                    val requestSnapshot = firestore.collection("friendRequests")
                        .whereEqualTo("senderId", senderId)
                        .whereEqualTo("receiverId", userId)
                        .get()
                        .await()

                    val hasValidPendingRequest = requestSnapshot.documents.any { doc ->
                        doc.getString("status") == "pending"
                    }

                    if (!hasValidPendingRequest) {
                        batch.delete(notificationDoc.reference)
                        deletedCount++
                    }
                } else {
                    batch.delete(notificationDoc.reference)
                    deletedCount++
                }
            }

            if (deletedCount > 0) {
                batch.commit().await()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     *
     * Data class per rappresentare il modello usato in Firebase
     *
     * Ha un metodo toNotifica() che converte in NotificationsViewModel.Notifica
     *
     */
    data class FirebaseNotifica(
        val id: String = "",
        val userId: String = "",
        val titolo: String = "",
        val descrizione: String = "",
        val tipo: String = "",
        val categoria: String = "",
        val timestamp: Timestamp = Timestamp.now(),
        val isLetta: Boolean = false,
        val rifugioId: String? = null,
        val utenteId: String? = null,
        val avatarUrl: String? = null,
        val achievementId: String? = null,
        val puntiGuadagnati: Int? = null
    ) {
        fun toNotifica(): NotificationsViewModel.Notifica {
            return NotificationsViewModel.Notifica(
                id = id,
                titolo = titolo,
                descrizione = descrizione,
                tipo = try {
                    NotificationsViewModel.TipoNotifica.valueOf(tipo)
                } catch (e: Exception) {
                    NotificationsViewModel.TipoNotifica.SISTEMA
                },
                tempo = formatTempo(timestamp),
                isLetta = isLetta,
                icona = getIconaFromTipo(tipo),
                rifugioId = rifugioId,
                utenteId = utenteId,
                achievementId = achievementId,
                puntiGuadagnati = puntiGuadagnati,
                avatarUrl = avatarUrl,
                timestamp = timestamp.seconds * 1000L
            )
        }

        /**
         *
         * Formatta il tempo
         *
         */
        private fun formatTempo(timestamp: Timestamp): String {
            val now = System.currentTimeMillis()
            val time = timestamp.toDate().time
            val diff = now - time

            return when {
                diff < 60_000 -> "Ora"
                diff < 3600_000 -> "${diff / 60_000} minuti fa"
                diff < 86400_000 -> "${diff / 3600_000} ore fa"
                diff < 2592000_000 -> "${diff / 86400_000} giorni fa"
                else -> "${diff / 2592000_000} mesi fa"
            }
        }

        /**
         *
         * Recupera le icone per il tipo di notifica
         *
         */
        private fun getIconaFromTipo(tipo: String): String {
            return try {
                when (NotificationsViewModel.TipoNotifica.valueOf(tipo)) {
                    NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA -> "ic_person_add_24"
                    NotificationsViewModel.TipoNotifica.NUOVO_MEMBRO_GRUPPO -> "ic_cabin_24"
                    NotificationsViewModel.TipoNotifica.NUOVA_SFIDA_MESE -> "ic_leaderboard_24"
                    NotificationsViewModel.TipoNotifica.DOPPIO_PUNTI_RIFUGI -> "ic_star_24"
                    NotificationsViewModel.TipoNotifica.SFIDA_COMPLETATA -> "ic_achievement_24"
                    NotificationsViewModel.TipoNotifica.PUNTI_OTTENUTI -> "ic_leaderboard_24"
                    NotificationsViewModel.TipoNotifica.TIMBRO_OTTENUTO -> "ic_stamp_24"
                    NotificationsViewModel.TipoNotifica.SISTEMA -> "ic_notifications_black_24dp"
                }
            } catch (e: Exception) {
                "ic_notifications_black_24dp"
            }
        }
    }
}