package com.example.mountainpassport_girarifugi.data.repository

import com.example.mountainpassport_girarifugi.ui.notifications.NotificationsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
        private const val COLLECTION_USERS = "users"
    }

    /**
     * Carica tutte le notifiche dell'utente corrente
     */
    suspend fun getAllNotifications(): List<NotificationsViewModel.Notifica> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            val snapshot = firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseNotifica::class.java)?.copy(id = doc.id)?.toNotifica()
            }
        } catch (e: Exception) {
            throw Exception("Errore nel caricamento delle notifiche: ${e.message}")
        }
    }

    /**
     * Observer per le notifiche in tempo reale
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifiche = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseNotifica::class.java)?.copy(id = doc.id)?.toNotifica()
                } ?: emptyList()

                trySend(notifiche)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Segna una notifica come letta
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
     * Segna tutte le notifiche come lette
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
     * Elimina una notifica
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
     * Crea una nuova notifica - FIX: ora salva correttamente l'ID
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
        puntiGuadagnati: Int? = null
    ): Boolean {
        return try {
            val docRef = firestore.collection(COLLECTION_NOTIFICATIONS).document()
            val notifica = FirebaseNotifica(
                id = docRef.id, // FIX: Salva l'ID del documento
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
                puntiGuadagnati = puntiGuadagnati
            )

            docRef.set(notifica).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Crea notifica di richiesta amicizia
     */
    suspend fun createFriendRequestNotification(
        receiverId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String? = null
    ): Boolean {
        return createNotification(
            userId = receiverId,
            titolo = senderName, // Il nome va nel titolo per le friend request
            descrizione = "Richiesta di amicizia", // Descrizione più semplice
            tipo = NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA,
            categoria = "amici",
            utenteId = senderId,
        )
    }

    /**
     * Crea notifica di amicizia accettata
     */
    suspend fun createFriendAcceptedNotification(
        receiverId: String,   // chi riceve la notifica (il mittente della richiesta originale)
        accepterName: String, // nome di chi ha accettato
        accepterId: String    // id di chi ha accettato
    ): Boolean {
        return createNotification(
            userId = receiverId, // il mittente della richiesta riceve la notifica
            titolo = "Richiesta accettata",
            descrizione = "$accepterName ha accettato la tua richiesta di amicizia",
            tipo = NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA,
            categoria = "amici",
            utenteId = accepterId
        )
    }

    /**
     * Crea notifica per nuovo rifugio
     */
    suspend fun createNewRifugioNotification(
        userId: String,
        rifugioId: String,
        rifugioName: String
    ): Boolean {
        return createNotification(
            userId = userId,
            titolo = "Nuovo rifugio disponibile",
            descrizione = "$rifugioName è ora disponibile per la visita!",
            tipo = NotificationsViewModel.TipoNotifica.NUOVO_MEMBRO_GRUPPO,
            categoria = "rifugi",
            rifugioId = rifugioId
        )
    }

    /**
     * Crea notifica per punti guadagnati
     */
    suspend fun createPointsEarnedNotification(
        userId: String,
        punti: Int,
        rifugioName: String?
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
            categoria = "punti",
            puntiGuadagnati = punti
        )
    }

    // Data class per Firebase
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
                tipo = NotificationsViewModel.TipoNotifica.valueOf(tipo),
                tempo = formatTempo(timestamp),
                isLetta = isLetta,
                icona = getIconaFromTipo(tipo),
                categoria = categoria,
                rifugioId = rifugioId,
                utenteId = utenteId,
                achievementId = achievementId,
                puntiGuadagnati = puntiGuadagnati
            )
        }

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

        private fun getIconaFromTipo(tipo: String): String {
            return when (NotificationsViewModel.TipoNotifica.valueOf(tipo)) {
                NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA -> "ic_person_add_24"
                NotificationsViewModel.TipoNotifica.NUOVO_MEMBRO_GRUPPO -> "ic_cabin_24"
                NotificationsViewModel.TipoNotifica.NUOVA_SFIDA_MESE -> "ic_leaderboard_24"
                NotificationsViewModel.TipoNotifica.DOPPIO_PUNTI_RIFUGI -> "ic_star_24"
                NotificationsViewModel.TipoNotifica.SFIDA_COMPLETATA -> "ic_achievement_24"
                NotificationsViewModel.TipoNotifica.PUNTI_OTTENUTI -> "ic_leaderboard_24"
                NotificationsViewModel.TipoNotifica.TIMBRO_OTTENUTO -> "ic_stamp_24"
                NotificationsViewModel.TipoNotifica.SISTEMA -> "ic_notifications_black_24dp"
            }
        }
    }
}