package com.example.mountainpassport_girarifugi.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Review(
    @DocumentId
    val id: String = "",
    val rifugioId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val images: List<String> = emptyList()
)

data class RifugioStats(
    val rifugioId: Int = 0,
    val averageRating: Float = 0f,
    val totalReviews: Int = 0,
    val totalVisits: Int = 0,
    val totalSaves: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
)

data class UserRifugioInteraction(
    val userId: String = "",
    val rifugioId: Int = 0,
    val isSaved: Boolean = false,
    val isVisited: Boolean = false,
    val visitDate: Timestamp? = null,
    val rating: Float? = null,
    val reviewId: String? = null,
    val lastInteraction: Timestamp = Timestamp.now()
)
