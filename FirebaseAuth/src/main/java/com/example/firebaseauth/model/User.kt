package com.example.firebaseauth.model

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val username: String? = null
)