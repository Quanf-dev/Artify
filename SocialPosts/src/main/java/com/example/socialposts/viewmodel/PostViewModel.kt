package com.example.socialposts.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.repository.AuthRepository
import com.example.socialposts.model.Post
import com.example.socialposts.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val authManager: FirebaseAuthManager
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _postCreationSuccess = MutableStateFlow(false)
    val postCreationSuccess: StateFlow<Boolean> = _postCreationSuccess.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            
            try {
                postRepository.getPosts()
                    .collectLatest { postsList ->
                        _posts.value = postsList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load posts: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun createPost(imageUri: Uri, caption: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _postCreationSuccess.value = false
            
            try {
                val currentUser = authManager.getCurrentUserWithUsername()
                if (currentUser == null) {
                    _errorMessage.value = "User not authenticated"
                    _isLoading.value = false
                    return@launch
                }
                
                Log.d("PostViewModel", "Creating post with user: ${currentUser.uid}, username: ${currentUser.username}, photoUrl: ${currentUser.photoUrl}")
                
                val result = postRepository.createPost(imageUri, caption)
                result.onSuccess {
                    _postCreationSuccess.value = true
                }.onFailure { error ->
                    _errorMessage.value = "Failed to create post: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create post: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLikePost(postId: String) {
        viewModelScope.launch {
            try {
                val currentUser = authManager.getCurrentUserWithUsername()
                if (currentUser != null) {
                    postRepository.toggleLikePost(postId, currentUser.uid)
                } else {
                    _errorMessage.value = "You need to be logged in to like posts"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle like: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetPostCreationState() {
        _postCreationSuccess.value = false
    }
} 