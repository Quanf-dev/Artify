package com.example.socialposts.repository

import android.net.Uri
import com.example.socialposts.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getPosts(): Flow<List<Post>>
    suspend fun createPost(imageUri: Uri, caption: String): Result<Post>
    suspend fun getPostById(postId: String): Result<Post>
    suspend fun toggleLikePost(postId: String, userId: String): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
} 