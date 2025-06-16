package com.example.socialposts.repository

import android.net.Uri
import android.util.Log
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.model.User
import com.example.firebaseauth.repository.AuthRepository
import com.example.socialposts.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val authRepository: AuthRepository,
    private val authManager: FirebaseAuthManager
) : PostRepository {

    private val postsCollection = firestore.collection("posts")
    private val storageRef = storage.reference.child("posts")

    override suspend fun getPosts(): Flow<List<Post>> = callbackFlow {
        val listenerRegistration = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                trySend(posts)
            }

        awaitClose { 
            listenerRegistration.remove() 
        }
    }

    override suspend fun createPost(imageUri: Uri, caption: String): Result<Post> = runCatching {
        // Use getCurrentUserWithUsername to get complete user data including username and photoUrl
        val currentUser = authManager.getCurrentUserWithUsername() ?: throw IllegalStateException("User not authenticated")
        
        Log.d("PostRepository", "Creating post for user: ${currentUser.uid}, username: ${currentUser.username}, photoUrl: ${currentUser.photoUrl}")
        
        // Upload image to Firebase Storage
        val randomId = UUID.randomUUID().toString()
        val imageRef = storageRef.child("$randomId.jpg")
        imageRef.putFile(imageUri).await()
        val imageUrl = imageRef.downloadUrl.await().toString()

        // Create post in Firestore with current user's information
        val post = Post(
            avatarUrl = currentUser.photoUrl ?: "https://ia903406.us.archive.org/5/items/49_20210404/15.png",
            caption = caption,
            imageUrl = imageUrl,
            likedBy = emptyList(), // Start with empty likes
            uid = currentUser.uid, // Use current user's UID
            username = currentUser.username ?: currentUser.displayName ?: "Anonymous" // Use current user's username or displayName
        )

        val documentRef = postsCollection.add(post).await()
        return@runCatching post.copy(id = documentRef.id)
    }

    override suspend fun getPostById(postId: String): Result<Post> = runCatching {
        val document = postsCollection.document(postId).get().await()
        document.toObject(Post::class.java) ?: throw IllegalStateException("Post not found")
    }

    override suspend fun toggleLikePost(postId: String, userId: String): Result<Unit> = runCatching {
        val postRef = postsCollection.document(postId)
        val post = postRef.get().await().toObject(Post::class.java) ?: throw IllegalStateException("Post not found")

        val likedBy = post.likedBy.toMutableList()
        if (likedBy.contains(userId)) {
            likedBy.remove(userId)
        } else {
            likedBy.add(userId)
        }

        postRef.update("likedBy", likedBy).await()
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val post = postsCollection.document(postId).get().await().toObject(Post::class.java)
            ?: throw IllegalStateException("Post not found")

        // Delete image from storage if it exists
        if (post.imageUrl.isNotEmpty()) {
            val imageRef = storage.getReferenceFromUrl(post.imageUrl)
            imageRef.delete().await()
        }

        // Delete post document
        postsCollection.document(postId).delete().await()
    }
} 