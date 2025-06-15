package com.example.artify.ui.verification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {

    private val _verificationState = MutableLiveData<EmailVerificationState>()
    val verificationState: LiveData<EmailVerificationState> = _verificationState
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Force refresh user data from server and then check verification status
     * This is used when the user clicks the "Check Verification" button
     */
    fun refreshAndCheckEmailVerification() {
        viewModelScope.launch {
            _verificationState.value = EmailVerificationState.Loading
            try {
                // Check if user is signed in
                if (!firebaseAuthManager.isUserSignedIn()) {
                    _verificationState.value = EmailVerificationState.Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                    return@launch
                }
                
                // Get current user before reload
                val currentUserBefore = firebaseAuthManager.getCurrentUser()
                android.util.Log.d("EmailVerificationVM", "User before forced reload: ${currentUserBefore?.email}, verified: ${currentUserBefore?.isEmailVerified}")
                
                if (currentUserBefore == null) {
                    _verificationState.value = EmailVerificationState.Error("Không tìm thấy người dùng đăng nhập")
                    return@launch
                }
                
                // Force sign out and sign in again to get fresh token
                val email = currentUserBefore.email
                val password = getPasswordFromSecureStorage() // Implement this method to get saved password
                
                if (email != null && password != null) {
                    // Sign out and sign in again to force refresh token
                    try {
                        // Sign out first
                        firebaseAuthManager.signOut()
                        
                        // Small delay to ensure sign out is complete
                        delay(500)
                        
                        // Sign in again to get fresh token
                        val result = firebaseAuthManager.signInWithEmailAndPassword(email, password)
                        
                        when (result) {
                            is FirebaseAuthResult.Success -> {
                                // Check verification status after fresh login
                                val currentUser = firebaseAuthManager.getCurrentUser()
                                android.util.Log.d("EmailVerificationVM", "User after fresh login: ${currentUser?.email}, verified: ${currentUser?.isEmailVerified}")
                                
                                if (currentUser != null) {
                                    if (currentUser.isEmailVerified) {
                                        _verificationState.value = EmailVerificationState.Verified
                                    } else {
                                        // Try direct API call as fallback
                                        checkEmailVerificationDirectly()
                                    }
                                } else {
                                    _verificationState.value = EmailVerificationState.Error("Không tìm thấy người dùng sau khi đăng nhập lại")
                                }
                            }
                            is FirebaseAuthResult.Error -> {
                                // Try direct API call as fallback
                                checkEmailVerificationDirectly()
                            }
                            is FirebaseAuthResult.Loading -> {
                                // Continue waiting
                            }
                        }
                    } catch (e: Exception) {
                        // Try direct API call as fallback
                        checkEmailVerificationDirectly()
                    }
                } else {
                    // Try direct API call as fallback
                    checkEmailVerificationDirectly()
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailVerificationVM", "Exception during forced verification check", e)
                _verificationState.value = EmailVerificationState.Error("Không thể cập nhật trạng thái xác thực từ máy chủ. Vui lòng thử lại sau.")
            }
        }
    }
    
    /**
     * Fallback method to check email verification status directly through Firebase Auth API
     */
    private suspend fun checkEmailVerificationDirectly() {
        try {
            // Try to get current Firebase user
            val firebaseUser = auth.currentUser
            
            if (firebaseUser == null) {
                _verificationState.value = EmailVerificationState.Error("Không thể xác định người dùng hiện tại")
                return
            }
            
            // Force reload user from server
            try {
                firebaseUser.reload().await()
                
                // Get fresh instance after reload
                val freshUser = auth.currentUser
                
                if (freshUser != null && freshUser.isEmailVerified) {
                    _verificationState.value = EmailVerificationState.Verified
                } else {
                    _verificationState.value = EmailVerificationState.NotVerified
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailVerificationVM", "Error reloading user directly", e)
                _verificationState.value = EmailVerificationState.Error("Không thể cập nhật trạng thái xác thực từ máy chủ sau nhiều lần thử.")
            }
        } catch (e: Exception) {
            android.util.Log.e("EmailVerificationVM", "Exception in direct verification check", e)
            _verificationState.value = EmailVerificationState.Error("Không thể cập nhật trạng thái xác thực từ máy chủ.")
        }
    }
    
    /**
     * Get password from secure storage if available
     * Note: This is a placeholder - you would need to implement secure password storage
     */
    private fun getPasswordFromSecureStorage(): String? {
        // In a real app, you would retrieve the password from secure storage
        // For security reasons, we don't actually want to store passwords
        // This is just a placeholder for the implementation
        return null
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _verificationState.value = EmailVerificationState.Loading
            try {
                // Kiểm tra xem user có đăng nhập không
                if (!firebaseAuthManager.isUserSignedIn()) {
                    android.util.Log.e("EmailVerificationVM", "User không đăng nhập")
                    _verificationState.value = EmailVerificationState.Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                    return@launch
                }
                
                // Kiểm tra user hiện tại trước khi reload
                val currentUserBefore = firebaseAuthManager.getCurrentUser()
                android.util.Log.d("EmailVerificationVM", "User trước khi reload: ${currentUserBefore?.email}, verified: ${currentUserBefore?.isEmailVerified}")
                
                if (currentUserBefore == null) {
                    _verificationState.value = EmailVerificationState.Error("Không tìm thấy người dùng đăng nhập")
                    return@launch
                }
                
                // Reload user từ Firebase để lấy trạng thái mới nhất
                val reloadResult = firebaseAuthManager.reloadCurrentUser()
                android.util.Log.d("EmailVerificationVM", "Kết quả reload: $reloadResult")
                
                when (reloadResult) {
                    is FirebaseAuthResult.Success -> {
                        val currentUser = firebaseAuthManager.getCurrentUser()
                        android.util.Log.d("EmailVerificationVM", "User sau khi reload: ${currentUser?.email}, verified: ${currentUser?.isEmailVerified}")
                        
                        if (currentUser != null) {
                            if (currentUser.isEmailVerified) {
                                _verificationState.value = EmailVerificationState.Verified
                            } else {
                                _verificationState.value = EmailVerificationState.NotVerified
                            }
                        } else {
                            _verificationState.value = EmailVerificationState.Error("Không tìm thấy người dùng sau khi reload")
                        }
                    }
                    is FirebaseAuthResult.Error -> {
                        android.util.Log.e("EmailVerificationVM", "Lỗi reload user: ${reloadResult.exception.message}")
                        _verificationState.value = EmailVerificationState.Error(reloadResult.exception.message ?: "Lỗi kiểm tra xác thực email")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _verificationState.value = EmailVerificationState.Loading
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailVerificationVM", "Exception trong checkEmailVerification", e)
                _verificationState.value = EmailVerificationState.Error(e.message ?: "Lỗi kiểm tra xác thực email")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _verificationState.value = EmailVerificationState.Loading
            try {
                val result = firebaseAuthManager.sendEmailVerification()
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _verificationState.value = EmailVerificationState.EmailSent
                    }
                    is FirebaseAuthResult.Error -> {
                        _verificationState.value = EmailVerificationState.Error(result.exception.message ?: "Gửi email xác thực thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _verificationState.value = EmailVerificationState.Loading
                    }
                }
            } catch (e: Exception) {
                _verificationState.value = EmailVerificationState.Error(e.message ?: "Gửi email xác thực thất bại")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                firebaseAuthManager.signOut()
            } catch (e: Exception) {
                // Log error but continue with logout
            }
        }
    }

    fun debugAuthState() {
        viewModelScope.launch {
            try {
                val isSignedIn = firebaseAuthManager.isUserSignedIn()
                val currentUser = firebaseAuthManager.getCurrentUser()
                
                android.util.Log.d("EmailVerificationVM", "=== DEBUG AUTH STATE ===")
                android.util.Log.d("EmailVerificationVM", "Is signed in: $isSignedIn")
                android.util.Log.d("EmailVerificationVM", "Current user: ${currentUser?.email}")
                android.util.Log.d("EmailVerificationVM", "Email verified: ${currentUser?.isEmailVerified}")
                android.util.Log.d("EmailVerificationVM", "User UID: ${currentUser?.uid}")
                android.util.Log.d("EmailVerificationVM", "========================")
                
                // Set đúng state dựa trên kết quả
                if (isSignedIn && currentUser != null) {
                    if (currentUser.isEmailVerified) {
                        _verificationState.value = EmailVerificationState.Verified
                    } else {
                        _verificationState.value = EmailVerificationState.NotVerified
                    }
                } else {
                    _verificationState.value = EmailVerificationState.Error("Không có người dùng đăng nhập")
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailVerificationVM", "Error in debugAuthState", e)
                _verificationState.value = EmailVerificationState.Error("Debug error: ${e.message}")
            }
        }
    }
}

sealed class EmailVerificationState {
    object Loading : EmailVerificationState()
    object Verified : EmailVerificationState()
    object NotVerified : EmailVerificationState()
    object EmailSent : EmailVerificationState()
    data class Error(val message: String) : EmailVerificationState()
} 