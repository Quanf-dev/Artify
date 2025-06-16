package com.example.artify.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artify.model.UserProfile
import com.example.firebaseauth.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authManager: FirebaseAuthManager
) : ViewModel() {
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Reset error state
            
            try {
                val user = authManager.getCurrentUserWithUsername()
                user?.let {
                    _userProfile.value = UserProfile(
                        uid = it.uid,
                        displayName = it.username ?: "User",
                        email = it.email,
                        phoneNumber = it.phoneNumber,
                        photoUrl = it.photoUrl,
                        isEmailVerified = it.isEmailVerified,
                        providerId = "firebase" // Since we don't have providerData in our User model
                    )
                } ?: run {
                    _error.value = "User not found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load user profile"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshUserProfile() {
        loadUserProfile()
    }
} 