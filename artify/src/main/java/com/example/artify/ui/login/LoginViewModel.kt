package com.example.artify.ui.login

import android.app.Activity
import android.util.Log
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
class LoginViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager,
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    var verificationId: String? = null
        private set

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = firebaseAuthManager.signInWithEmailAndPassword(email, password)
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        // Kiểm tra xem email đã được xác thực chưa
                        if (result.data.isEmailVerified) {
                            _loginState.value = LoginState.Success(result.data)
                        } else {
                            _loginState.value = LoginState.EmailNotVerified
                        }
                    }
                    is FirebaseAuthResult.Error -> {
                        _loginState.value = LoginState.Error(result.exception.message ?: "Đăng nhập thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun loginWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Lấy intent đăng nhập Google
                val signInIntent = firebaseAuthManager.getGoogleSignInIntent()
                activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
                // Kết quả sẽ được xử lý trong onActivityResult của Activity
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Đăng nhập Google thất bại")
            }
        }
    }

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                Log.d("LoginViewModel", "Xử lý kết quả đăng nhập Google")
                // Xử lý kết quả đăng nhập Google
                val result = firebaseAuthManager.handleGoogleSignInResult(data)
                Log.d("LoginViewModel", "Kết quả đăng nhập Google: $result")
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _loginState.value = LoginState.Success(result.data)
                    }
                    is FirebaseAuthResult.Error -> {
                        _loginState.value = LoginState.Error(result.exception.message ?: "Đăng nhập Google thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Lỗi xử lý đăng nhập Google", e)
                _loginState.value = LoginState.Error(e.message ?: "Đăng nhập Google thất bại")
            }
        }
    }

    fun loginWithPhone(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Gửi mã xác thực đến số điện thoại
                firebaseAuthManager.sendPhoneVerificationCode(
                    phoneNumber,
                    activity,
                    onCodeSent = { verificationId ->
                        this@LoginViewModel.verificationId = verificationId
                        _loginState.value = LoginState.PhoneVerificationSent
                    },
                    onVerificationFailed = { e ->
                        _loginState.value = LoginState.Error(e.message ?: "Gửi mã xác thực thất bại")
                    }
                )
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Gửi mã xác thực thất bại")
            }
        }
    }

    fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = firebaseAuthManager.verifyPhoneNumberWithCode(verificationId, code)
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _loginState.value = LoginState.Success(result.data)
                    }
                    is FirebaseAuthResult.Error -> {
                        _loginState.value = LoginState.Error(result.exception.message ?: "Xác thực mã thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Xác thực mã thất bại")
            }
        }
    }

    fun loginWithFacebook(activity: Activity) {
        _loginState.value = LoginState.Loading
        firebaseAuthManager.loginWithFacebook(
            activity = activity,
            onSuccess = { user ->
                _loginState.value = LoginState.Success(user)
            },
            onError = { exception ->
                _loginState.value = LoginState.Error(exception.message ?: "Đăng nhập Facebook thất bại")
            },
            onCancel = {
                _loginState.value = LoginState.Error("Đăng nhập Facebook bị hủy")
            }
        )
    }

    fun handleFacebookActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        firebaseAuthManager.handleFacebookActivityResult(requestCode, resultCode, data)
    }
    
    companion object {
        const val GOOGLE_SIGN_IN_REQUEST_CODE = 9001
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: Any) : LoginState()
    data class Error(val message: String) : LoginState()
    object PhoneVerificationSent : LoginState()
    object PasswordResetEmailSent : LoginState()
    object EmailVerificationSent : LoginState()
    object EmailNotVerified : LoginState()
}