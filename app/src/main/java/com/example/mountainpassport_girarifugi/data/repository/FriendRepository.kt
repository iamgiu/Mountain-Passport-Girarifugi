package com.example.mountainpassport_girarifugi.data.repository

import android.content.Context
import com.example.mountainpassport_girarifugi.ui.profile.Friend
import com.example.mountainpassport_girarifugi.ui.profile.FriendRequest
import com.example.mountainpassport_girarifugi.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsRepository = NotificationsRepository()

    fun sendFriendRequest(receiverId: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, "Utente non autenticato")
            return
        }

        val senderId = currentUser.uid

        checkIfAlreadyFriends(senderId, receiverId) { alreadyFriends ->
            if (alreadyFriends) {
                callback(false, "Gia' amici")
                return@checkIfAlreadyFriends
            }

            checkIfRequestExists(senderId, receiverId) { requestExists ->
                if (requestExists) {
                    callback(false, "Richiesta gia' inviata")
                    return@checkIfRequestExists
                }

                firestore.collection("users")
                    .document(senderId)
                    .get()
                    .addOnSuccessListener { senderDoc ->
                        val senderUser = senderDoc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)

                        if (senderUser != null) {
                            val requestId = firestore.collection("friendRequests").document().id
                            val friendRequest = FriendRequest(
                                id = requestId,
                                senderId = senderId,
                                receiverId = receiverId,
                                senderName = "${senderUser.nome} ${senderUser.cognome}".trim(),
                                senderNickname = senderUser.nickname,
                                senderAvatarUrl = senderUser.profileImageUrl ?: "",
                                status = "pending",
                                timestamp = System.currentTimeMillis()
                            )

                            firestore.collection("friendRequests")
                                .document(requestId)
                                .set(friendRequest)
                                .addOnSuccessListener {
                                    // Create the notification
                                    CoroutineScope(Dispatchers.IO).launch {
                                        notificationsRepository.createFriendRequestNotification(
                                            receiverId = receiverId,
                                            senderId = senderId,
                                            senderName = "${senderUser.nome} ${senderUser.cognome}".trim(),
                                            senderAvatarUrl = senderUser.profileImageUrl ?: ""
                                        )
                                    }
                                    callback(true, null)
                                }
                                .addOnFailureListener { e ->
                                    callback(false, "Errore nell'invio: ${e.message}")
                                }
                        } else {
                            callback(false, "Errore nel recuperare i dati del mittente")
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, "Errore nel recuperare i dati: ${e.message}")
                    }
            }
        }
    }

    fun listenForFriendRequests(callback: (List<FriendRequest>) -> Unit): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null

        return firestore.collection("friendRequests")
            .whereEqualTo("receiverId", currentUser.uid)
            .whereEqualTo("status", "pending")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)
                } ?: emptyList()

                callback(requests)
            }
    }

    fun acceptFriendRequestByUserId(senderId: String, callback: (Boolean, String?) -> Unit) {
        android.util.Log.d("FriendRepository", "acceptFriendRequestByUserId called with senderId: $senderId")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, "Utente non autenticato")
            return
        }

        val currentUserId = currentUser.uid

        if (senderId == currentUserId) {
            callback(false, "Non puoi accettare una richiesta da te stesso")
            return
        }

        if (senderId.isBlank()) {
            callback(false, "ID utente non valido")
            return
        }

        // PRIMA: Controlla se sono già amici
        checkIfAlreadyFriends(senderId, currentUserId) { alreadyFriends ->
            if (alreadyFriends) {
                android.util.Log.d("FriendRepository", "Users are already friends")
                callback(true, null) // Considera come successo se sono già amici
                return@checkIfAlreadyFriends
            }

            // TROVA LA RICHIESTA PENDENTE
            firestore.collection("friendRequests")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", currentUserId)
                .get() // Rimuovi il filtro per status per trovare tutte le richieste
                .addOnSuccessListener { snapshot ->
                    android.util.Log.d("FriendRepository", "Found ${snapshot.documents.size} requests")

                    if (snapshot.documents.isEmpty()) {
                        android.util.Log.e("FriendRepository", "No request found between users")
                        callback(false, "Nessuna richiesta trovata")
                        return@addOnSuccessListener
                    }

                    // TROVA LA RICHIESTA PIÙ RECENTE
                    val mostRecentRequest = snapshot.documents
                        .mapNotNull { doc ->
                            val data = doc.data
                            if (data != null) {
                                val timestamp = data["timestamp"] as? Long ?: 0L
                                doc to timestamp
                            } else null
                        }
                        .maxByOrNull { it.second }
                        ?.first

                    if (mostRecentRequest == null) {
                        callback(false, "Richiesta non valida")
                        return@addOnSuccessListener
                    }

                    val currentStatus = mostRecentRequest.getString("status") ?: "unknown"
                    android.util.Log.d("FriendRepository", "Request status: $currentStatus")

                    when (currentStatus) {
                        "pending" -> {
                            // Procedi con l'accettazione
                            processAcceptance(mostRecentRequest.id, senderId, currentUserId, callback)
                        }
                        "accepted" -> {
                            // Già accettata, controlla se l'amicizia esiste
                            android.util.Log.d("FriendRepository", "Request already accepted, checking friendship")
                            ensureFriendshipExists(senderId, currentUserId) { friendshipExists ->
                                if (friendshipExists) {
                                    callback(true, null)
                                } else {
                                    // Amicizia non esiste, ricreala
                                    addFriendshipConnectionSafe(senderId, currentUserId) { success ->
                                        callback(success, if (success) null else "Errore nel ricreare l'amicizia")
                                    }
                                }
                            }
                        }
                        "declined" -> {
                            callback(false, "Richiesta precedentemente rifiutata")
                        }
                        else -> {
                            callback(false, "Stato richiesta sconosciuto: $currentStatus")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FriendRepository", "Error querying requests", e)
                    callback(false, "Errore nella ricerca: ${e.message}")
                }
        }
    }

    // NUOVO METODO: Processa l'accettazione
    private fun processAcceptance(
        requestId: String,
        senderId: String,
        receiverId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        android.util.Log.d("FriendRepository", "Processing acceptance for request: $requestId")

        firestore.runTransaction { transaction ->
            val requestRef = firestore.collection("friendRequests").document(requestId)
            val requestSnapshot = transaction.get(requestRef)

            if (!requestSnapshot.exists()) {
                throw Exception("Richiesta non trovata")
            }

            val currentStatus = requestSnapshot.getString("status")
            if (currentStatus == "accepted") {
                throw Exception("ALREADY_ACCEPTED") // Speciale per identificare questo caso
            }

            if (currentStatus != "pending") {
                throw Exception("Richiesta non più pendente (status: $currentStatus)")
            }

            // Aggiorna lo status
            transaction.update(requestRef, mapOf(
                "status" to "accepted",
                "acceptedAt" to System.currentTimeMillis()
            ))

            null
        }.addOnSuccessListener {
            android.util.Log.d("FriendRepository", "Transaction successful")

            // Crea l'amicizia
            addFriendshipConnectionSafe(senderId, receiverId) { success ->
                if (success) {
                    createAcceptanceNotificationAsync(receiverId, senderId)
                    callback(true, null)
                } else {
                    callback(false, "Errore nel creare l'amicizia")
                }
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("FriendRepository", "Transaction failed: ${e.message}")

            if (e.message == "ALREADY_ACCEPTED") {
                // Caso speciale: già accettata, verifica l'amicizia
                ensureFriendshipExists(senderId, receiverId) { friendshipExists ->
                    callback(friendshipExists, if (!friendshipExists) "Amicizia non trovata" else null)
                }
            } else {
                callback(false, "Errore nella transazione: ${e.message}")
            }
        }
    }

    // NUOVO METODO: Verifica che l'amicizia esista davvero
    private fun ensureFriendshipExists(
        userId1: String,
        userId2: String,
        callback: (Boolean) -> Unit
    ) {
        android.util.Log.d("FriendRepository", "Checking if friendship exists: $userId1 <-> $userId2")

        firestore.collection("users")
            .document(userId1)
            .collection("friends")
            .document(userId2)
            .get()
            .addOnSuccessListener { doc ->
                val exists = doc.exists()
                android.util.Log.d("FriendRepository", "Friendship exists: $exists")
                callback(exists)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FriendRepository", "Error checking friendship", e)
                callback(false)
            }
    }

    // CORREZIONE del metodo checkIfAlreadyFriends per essere più robusto
    private fun checkIfAlreadyFriends(userId1: String, userId2: String, callback: (Boolean) -> Unit) {
        android.util.Log.d("FriendRepository", "Checking if already friends: $userId1 -> $userId2")

        firestore.collection("users")
            .document(userId1)
            .collection("friends")
            .document(userId2)
            .get()
            .addOnSuccessListener { doc ->
                val areFriends = doc.exists()
                android.util.Log.d("FriendRepository", "Already friends: $areFriends")
                callback(areFriends)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FriendRepository", "Error checking friendship status", e)
                // In caso di errore, assume che non siano amici per procedere
                callback(false)
            }
    }

    // Helper class per gestire callback multipli
    private class CallbackHandler(private val originalCallback: (Boolean, String?) -> Unit) {
        @Volatile
        private var invoked = false

        fun invoke(success: Boolean, error: String?) {
            synchronized(this) {
                if (!invoked) {
                    invoked = true
                    try {
                        originalCallback(success, error)
                    } catch (e: Exception) {
                        android.util.Log.e("FriendRepository", "Exception in callback", e)
                    }
                }
            }
        }
    }

    private fun addFriendshipConnectionSafe(
        userId1: String,
        userId2: String,
        callback: (Boolean) -> Unit
    ) {
        android.util.Log.d("FriendRepository", "addFriendshipConnectionSafe: $userId1 -> $userId2")

        // Validazione input
        if (userId1.isBlank() || userId2.isBlank() || userId1 == userId2) {
            android.util.Log.e("FriendRepository", "Invalid user IDs")
            callback(false)
            return
        }

        // PRIMO: Ottieni i documenti degli utenti
        val user1Ref = firestore.collection("users").document(userId1)
        val user2Ref = firestore.collection("users").document(userId2)

        user1Ref.get().addOnSuccessListener { user1Doc ->
            if (!user1Doc.exists()) {
                android.util.Log.e("FriendRepository", "User1 not found: $userId1")
                callback(false)
                return@addOnSuccessListener
            }

            user2Ref.get().addOnSuccessListener { user2Doc ->
                if (!user2Doc.exists()) {
                    android.util.Log.e("FriendRepository", "User2 not found: $userId2")
                    callback(false)
                    return@addOnSuccessListener
                }

                try {
                    val user1Data = user1Doc.data ?: emptyMap()
                    val user2Data = user2Doc.data ?: emptyMap()

                    // Crea oggetti Friend con null safety
                    val friend1 = createFriendSafely(userId1, user1Data)
                    val friend2 = createFriendSafely(userId2, user2Data)

                    // SECONDO: Crea una nuova batch DOPO aver ottenuto i dati
                    val batch = firestore.batch()

                    // Batch operations
                    batch.set(
                        firestore.collection("users")
                            .document(userId1)
                            .collection("friends")
                            .document(userId2),
                        friend2
                    )

                    batch.set(
                        firestore.collection("users")
                            .document(userId2)
                            .collection("friends")
                            .document(userId1),
                        friend1
                    )

                    // Global friends collection
                    val friendData = mapOf(
                        "user1" to userId1,
                        "user2" to userId2,
                        "since" to System.currentTimeMillis()
                    )

                    batch.set(
                        firestore.collection("friends").document("${userId1}_${userId2}"),
                        friendData
                    )

                    // COMMIT la batch
                    batch.commit().addOnSuccessListener {
                        android.util.Log.d("FriendRepository", "Friendship created successfully")
                        callback(true)
                    }.addOnFailureListener { e ->
                        android.util.Log.e("FriendRepository", "Error in batch commit", e)
                        callback(false)
                    }

                } catch (e: Exception) {
                    android.util.Log.e("FriendRepository", "Exception creating friendship objects", e)
                    callback(false)
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("FriendRepository", "Error fetching user2", e)
                callback(false)
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("FriendRepository", "Error fetching user1", e)
            callback(false)
        }
    }

    // Helper per creare oggetti Friend in modo sicuro
    private fun createFriendSafely(userId: String, userData: Map<String, Any>): Friend {
        val nome = userData["nome"] as? String ?: ""
        val cognome = userData["cognome"] as? String ?: ""
        val nickname = userData["nickname"] as? String ?: ""
        val profileImageUrl = userData["profileImageUrl"] as? String

        val fullName = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Utente"

        return Friend(
            userId = userId,
            fullName = fullName,
            nickname = nickname,
            profileImageUrl = profileImageUrl ?: "",
            addedTimestamp = System.currentTimeMillis()
        )
    }

    // Crea notifica di accettazione in modo async e non bloccante
    private fun createAcceptanceNotificationAsync(receiverId: String, senderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accepterDoc = firestore.collection("users")
                    .document(receiverId)
                    .get()
                    .await()

                val accepterData = accepterDoc.data
                if (accepterData != null) {
                    val nome = accepterData["nome"] as? String ?: ""
                    val cognome = accepterData["cognome"] as? String ?: ""
                    val accepterName = "$nome $cognome".trim().takeIf { it.isNotBlank() } ?: "Un utente"

                    notificationsRepository.createFriendAcceptedNotification(
                        receiverId = senderId,
                        accepterName = accepterName,
                        accepterId = receiverId
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("FriendRepository", "Failed to create acceptance notification", e)
                // Non propagare l'errore perché è operazione secondaria
            }
        }
    }

    fun acceptFriendRequest(requestId: String, callback: (Boolean, String?) -> Unit) {
        android.util.Log.d("FriendRepository", "acceptFriendRequest called with requestId: $requestId")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.util.Log.e("FriendRepository", "User not authenticated")
            callback(false, "Utente non autenticato")
            return
        }

        val currentUserId = currentUser.uid

        firestore.collection("friendRequests")
            .document(requestId)
            .get()
            .addOnSuccessListener { doc ->
                android.util.Log.d("FriendRepository", "Request document retrieved: ${doc.exists()}")

                val request = doc.toObject(FriendRequest::class.java)
                if (request == null) {
                    android.util.Log.e("FriendRepository", "Request is null")
                    callback(false, "Richiesta non trovata")
                    return@addOnSuccessListener
                }

                if (request.receiverId != currentUserId) {
                    android.util.Log.e("FriendRepository", "Request receiver mismatch")
                    callback(false, "Richiesta non valida")
                    return@addOnSuccessListener
                }

                android.util.Log.d("FriendRepository", "Request validated, proceeding with acceptance")

                // Usa una variabile atomica per evitare callback multipli
                val callbackHandler = object {
                    @Volatile
                    var invoked = false

                    fun invoke(success: Boolean, error: String?) {
                        synchronized(this) {
                            if (!invoked) {
                                invoked = true
                                callback(success, error)
                            }
                        }
                    }
                }

                // Prima aggiorna lo status della richiesta
                firestore.collection("friendRequests")
                    .document(requestId)
                    .update("status", "accepted")
                    .addOnSuccessListener {
                        android.util.Log.d("FriendRepository", "Request status updated to accepted")

                        // Poi aggiungi la connessione di amicizia
                        addFriendshipConnection(request.senderId, request.receiverId, request) { friendshipSuccess ->
                            android.util.Log.d("FriendRepository", "Friendship connection result: $friendshipSuccess")

                            if (friendshipSuccess) {
                                // Crea la notifica di accettazione in background (non bloccante)
                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val accepterDoc = firestore.collection("users")
                                                .document(currentUserId)
                                                .get()
                                                .await()

                                            val accepterUser = accepterDoc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
                                            if (accepterUser != null) {
                                                notificationsRepository.createFriendAcceptedNotification(
                                                    receiverId = request.senderId,
                                                    accepterName = "${accepterUser.nome} ${accepterUser.cognome}".trim(),
                                                    accepterId = currentUserId
                                                )
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.w("FriendRepository", "Failed to create acceptance notification", e)
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("FriendRepository", "Error in notification creation", e)
                                }

                                callbackHandler.invoke(true, null)
                            } else {
                                callbackHandler.invoke(false, "Errore nel creare la connessione di amicizia")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FriendRepository", "Failed to update request status", e)
                        callbackHandler.invoke(false, "Errore nell'aggiornamento della richiesta: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FriendRepository", "Failed to retrieve request document", e)
                callback(false, "Errore nel recupero: ${e.message}")
            }
    }

    fun declineFriendRequest(requestId: String, callback: (Boolean, String?) -> Unit) {
        firestore.collection("friendRequests")
            .document(requestId)
            .update("status", "declined")
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, "Errore nel rifiutare la richiesta: ${e.message}")
            }
    }

    // Rifiuta richiesta per User ID
    fun declineFriendRequestByUserId(senderId: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return callback(false, "Utente non autenticato")

        // Trova la richiesta pendente
        firestore.collection("friendRequests")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", currentUser.uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                val request = snapshot.documents.firstOrNull()
                if (request != null) {
                    // Usa il metodo esistente con l'ID trovato
                    declineFriendRequest(request.id, callback)
                } else {
                    callback(false, "Richiesta non trovata")
                }
            }
            .addOnFailureListener { e ->
                callback(false, "Errore nella ricerca: ${e.message}")
            }
    }

    private fun addFriendshipConnection(userId1: String, userId2: String, request: FriendRequest, callback: (Boolean) -> Unit) {
        android.util.Log.d("FriendRepository", "addFriendshipConnection: $userId1 -> $userId2")

        // Validazione input
        if (userId1.isBlank() || userId2.isBlank()) {
            android.util.Log.e("FriendRepository", "Invalid user IDs")
            callback(false)
            return
        }

        // Get user info for both users
        firestore.collection("users")
            .document(userId1)
            .get()
            .addOnSuccessListener { user1Doc ->
                if (!user1Doc.exists()) {
                    android.util.Log.e("FriendRepository", "User1 document not found: $userId1")
                    callback(false)
                    return@addOnSuccessListener
                }

                firestore.collection("users")
                    .document(userId2)
                    .get()
                    .addOnSuccessListener { user2Doc ->
                        if (!user2Doc.exists()) {
                            android.util.Log.e("FriendRepository", "User2 document not found: $userId2")
                            callback(false)
                            return@addOnSuccessListener
                        }

                        try {
                            val user1 = user1Doc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
                            val user2 = user2Doc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)

                            if (user1 == null || user2 == null) {
                                android.util.Log.e("FriendRepository", "Failed to parse user objects")
                                callback(false)
                                return@addOnSuccessListener
                            }

                            android.util.Log.d("FriendRepository", "Both users found, creating friendship")

                            val batch = firestore.batch()

                            // Create Friend objects with null checks
                            val friend1 = Friend(
                                userId = userId1,
                                fullName = "${user1.nome ?: ""} ${user1.cognome ?: ""}".trim().takeIf { it.isNotBlank() } ?: "Utente",
                                nickname = user1.nickname ?: "",
                                profileImageUrl = user1.profileImageUrl ?: "",
                                addedTimestamp = System.currentTimeMillis()
                            )

                            val friend2 = Friend(
                                userId = userId2,
                                fullName = "${user2.nome ?: ""} ${user2.cognome ?: ""}".trim().takeIf { it.isNotBlank() } ?: "Utente",
                                nickname = user2.nickname ?: "",
                                profileImageUrl = user2.profileImageUrl ?: "",
                                addedTimestamp = System.currentTimeMillis()
                            )

                            // Add to subcollections
                            batch.set(
                                firestore.collection("users")
                                    .document(userId1)
                                    .collection("friends")
                                    .document(userId2),
                                friend2
                            )

                            batch.set(
                                firestore.collection("users")
                                    .document(userId2)
                                    .collection("friends")
                                    .document(userId1),
                                friend1
                            )

                            // Add to global friends collection
                            val friendData = mapOf(
                                "user1" to userId1,
                                "user2" to userId2,
                                "since" to System.currentTimeMillis()
                            )

                            batch.set(
                                firestore.collection("friends").document("${userId1}_${userId2}"),
                                friendData
                            )

                            batch.set(
                                firestore.collection("friends").document("${userId2}_${userId1}"),
                                friendData
                            )

                            // Commit the batch
                            batch.commit()
                                .addOnSuccessListener {
                                    android.util.Log.d("FriendRepository", "Friendship created successfully")
                                    callback(true)
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("FriendRepository", "Error creating friendship", e)
                                    callback(false)
                                }

                        } catch (e: Exception) {
                            android.util.Log.e("FriendRepository", "Exception in addFriendshipConnection", e)
                            callback(false)
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FriendRepository", "Error fetching user2", e)
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FriendRepository", "Error fetching user1", e)
                callback(false)
            }
    }

    private fun checkIfRequestExists(senderId: String, receiverId: String, callback: (Boolean) -> Unit) {
        firestore.collection("friendRequests")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                callback(!snapshot.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}