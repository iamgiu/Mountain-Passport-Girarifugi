package com.example.mountainpassport_girarifugi.data.repository

import com.example.mountainpassport_girarifugi.ui.profile.Friend
import com.example.mountainpassport_girarifugi.ui.profile.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FriendRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun sendFriendRequest(receiverId: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, "Utente non autenticato")
            return
        }

        val senderId = currentUser.uid

        // Check if users are already friends
        checkIfAlreadyFriends(senderId, receiverId) { alreadyFriends ->
            if (alreadyFriends) {
                callback(false, "Gia' amici")
                return@checkIfAlreadyFriends
            }

            // Check if request already exists
            checkIfRequestExists(senderId, receiverId) { requestExists ->
                if (requestExists) {
                    callback(false, "Richiesta gia' inviata")
                    return@checkIfRequestExists
                }

                // Get sender info
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
                                status = "pending",
                                timestamp = System.currentTimeMillis()
                            )

                            firestore.collection("friendRequests")
                                .document(requestId)
                                .set(friendRequest)
                                .addOnSuccessListener {
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
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, "Utente non autenticato")
            return
        }

        // Get the friend request
        firestore.collection("friendRequests")
            .document(requestId)
            .get()
            .addOnSuccessListener { requestDoc ->
                val request = requestDoc.toObject(FriendRequest::class.java)
                if (request == null) {
                    callback(false, "Richiesta non trovata")
                    return@addOnSuccessListener
                }

                // Update request status to accepted
                firestore.collection("friendRequests")
                    .document(requestId)
                    .update("status", "accepted")
                    .addOnSuccessListener {
                        // Add to both users' friends collections
                        addFriendshipConnection(request.senderId, request.receiverId, request) { success ->
                            callback(success, if (success) null else "Errore nell'aggiungere l'amicizia")
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, "Errore nell'accettare la richiesta: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                callback(false, "Errore nel recuperare la richiesta: ${e.message}")
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