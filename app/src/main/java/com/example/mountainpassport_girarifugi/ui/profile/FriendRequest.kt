package com.example.mountainpassport_girarifugi.ui.profile

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val senderNickname: String = "",
    val status: String = "pending", // Saranno "pending", "accepted", "declined"
    val timestamp: Long = System.currentTimeMillis(),
    val senderAvatarUrl: String = ""
)