package com.example.artify.ui.verification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {

    private val _verificationState = MutableLiveData<EmailVerificationState>()
    val verificationState: LiveData<EmailVerificationState> = _verificationState

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