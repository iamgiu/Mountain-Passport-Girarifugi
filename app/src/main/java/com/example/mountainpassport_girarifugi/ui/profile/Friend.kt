package com.example.mountainpassport_girarifugi.ui.profile

data class Friend(
    val userId: String = "",
    val fullName: String = "",
    val nickname: String = "",
    val profileImageUrl: String = "",
    val addedTimestamp: Long = System.currentTimeMillis()
)