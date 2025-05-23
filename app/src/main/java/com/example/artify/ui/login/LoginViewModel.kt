package com.example.artify.ui.login

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuthManager: FirebaseAuthManager,
    private val savedStateHandle: SavedStateHandle  // Add this parameter
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = firebaseAuthManager.signInWithEmailAndPassword(email, password)
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _loginState.value = LoginState.Success(result.data)
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

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = firebaseAuthManager.createUserWithEmailAndPassword(email, password)
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _loginState.value = LoginState.Success(result.data)
                    }
                    is FirebaseAuthResult.Error -> {
                        _loginState.value = LoginState.Error(result.exception.message ?: "Đăng ký thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Đăng ký thất bại")
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
                    onCodeSent = { verificationId ->
                        // Lưu verificationId để sử dụng khi xác thực mã
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

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = firebaseAuthManager.sendPasswordResetEmail(email)
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _loginState.value = LoginState.PasswordResetEmailSent
                    }
                    is FirebaseAuthResult.Error -> {
                        _loginState.value = LoginState.Error(result.exception.message ?: "Gửi email đặt lại mật khẩu thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Gửi email đặt lại mật khẩu thất bại")
            }
        }
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
}