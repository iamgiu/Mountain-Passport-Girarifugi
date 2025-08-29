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

class FriendRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsRepository = NotificationsRepository()
    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context
    }

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
                                senderAvatarUrl = senderUser.profileImageUrl,
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
                                            senderAvatarUrl = senderUser.profileImageUrl
                                        )
                                    }
                                    
                                    // Mostra notifica locale se l'utente è il ricevente
                                    context?.let { ctx ->
                                        if (receiverId == currentUser.uid) {
                                            NotificationHelper.showFriendRequestNotification(
                                                ctx,
                                                "${senderUser.nome} ${senderUser.cognome}".trim()
                                            )
                                        }
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

    fun acceptFriendRequest(requestId: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return callback(false, "Utente non autenticato")

        firestore.collection("friendRequests")
            .document(requestId)
            .get()
            .addOnSuccessListener { doc ->
                val request = doc.toObject(FriendRequest::class.java)
                if (request != null && request.receiverId == currentUser.uid) {
                    val batch = firestore.batch()

                    val senderFriendRef = firestore.collection("friends")
                        .document("${request.senderId}_${request.receiverId}")
                    val receiverFriendRef = firestore.collection("friends")
                        .document("${request.receiverId}_${request.senderId}")

                    val friendData = mapOf(
                        "user1" to request.senderId,
                        "user2" to request.receiverId,
                        "since" to System.currentTimeMillis()
                    )

                    batch.set(senderFriendRef, friendData)
                    batch.set(receiverFriendRef, friendData)
                    batch.update(doc.reference, "status", "accepted")

                    batch.commit()
                        .addOnSuccessListener {
                            firestore.collection("users")
                                .document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { accepterDoc ->
                                    val accepterUser = accepterDoc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
                                    if (accepterUser != null) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            notificationsRepository.createFriendAcceptedNotification(
                                                receiverId = request.senderId,
                                                accepterName = "${accepterUser.nome} ${accepterUser.cognome}".trim(),
                                                accepterId = currentUser.uid
                                            )
                                        }
                                    }
                                }
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            callback(false, "Errore: ${e.message}")
                        }
                } else {
                    callback(false, "Richiesta non valida")
                }
            }
            .addOnFailureListener { e ->
                callback(false, "Errore nel recupero: ${e.message}")
            }
    }

    fun acceptFriendRequestByUserId(senderId: String, callback: (Boolean, String?) -> Unit) {
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
                    acceptFriendRequest(request.id, callback)
                } else {
                    callback(false, "Richiesta non trovata")
                }
            }
            .addOnFailureListener { e ->
                callback(false, "Errore nella ricerca: ${e.message}")
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

    // NUOVO METODO MANCANTE: Rifiuta richiesta per User ID
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
        val batch = firestore.batch()

        // Get user info for both users
        firestore.collection("users")
            .document(userId1)
            .get()
            .addOnSuccessListener { user1Doc ->
                firestore.collection("users")
                    .document(userId2)
                    .get()
                    .addOnSuccessListener { user2Doc ->
                        val user1 = user1Doc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
                        val user2 = user2Doc.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)

                        if (user1 != null && user2 != null) {
                            // Add user1 to user2's friends
                            val friend1 = Friend(
                                userId = userId1,
                                fullName = "${user1.nome} ${user1.cognome}".trim(),
                                nickname = user1.nickname,
                                profileImageUrl = user1.profileImageUrl,
                                addedTimestamp = System.currentTimeMillis()
                            )

                            // Add user2 to user1's friends
                            val friend2 = Friend(
                                userId = userId2,
                                fullName = "${user2.nome} ${user2.cognome}".trim(),
                                nickname = user2.nickname,
                                profileImageUrl = user2.profileImageUrl,
                                addedTimestamp = System.currentTimeMillis()
                            )

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

                            batch.commit()
                                .addOnSuccessListener {
                                    callback(true)
                                }
                                .addOnFailureListener {
                                    callback(false)
                                }
                        } else {
                            callback(false)
                        }
                    }
                    .addOnFailureListener {
                        callback(false)
                    }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun checkIfAlreadyFriends(userId1: String, userId2: String, callback: (Boolean) -> Unit) {
        firestore.collection("users")
            .document(userId1)
            .collection("friends")
            .document(userId2)
            .get()
            .addOnSuccessListener { doc ->
                callback(doc.exists())
            }
            .addOnFailureListener {
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