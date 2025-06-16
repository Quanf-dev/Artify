package com.example.socialposts.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @DocumentId
    val id: String = "",
    val avatarUrl: String = "https://ia903406.us.archive.org/5/items/49_20210404/15.png",
    val caption: String = "I create it easily by artify",
    val imageUrl: String = "",
    val likedBy: List<String> = listOf(),
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val uid: String = "",
    val username: String = ""
) {
    fun isLikedByUser(userId: String): Boolean = likedBy.contains(userId)
    
    fun getFormattedTimestamp(): String {
        val date = timestamp?.toDate() ?: Date()
        // Simple formatter for now, can be enhanced for better user experience
        return android.text.format.DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
} 