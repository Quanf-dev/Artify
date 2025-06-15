package com.example.artify.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val providerId: String
) {
    val contactInfo: String
        get() = when {
            providerId.contains("facebook") -> "Facebook"
            !phoneNumber.isNullOrEmpty() -> phoneNumber
            !email.isNullOrEmpty() -> email
            else -> "No contact info"
        }
} 