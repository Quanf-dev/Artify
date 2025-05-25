package com.example.artify.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artify.ui.login.LoginState
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {

    private val _registerState = MutableLiveData<LoginState>()
    val registerState: LiveData<LoginState> = _registerState

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = LoginState.Loading
            try {
                val result = firebaseAuthManager.createUserWithEmailAndPassword(email, password)
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _registerState.value = LoginState.Success(result.data)
                    }
                    is FirebaseAuthResult.Error -> {
                        _registerState.value = LoginState.Error(result.exception.message ?: "Đăng ký thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                        _registerState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                _registerState.value = LoginState.Error(e.message ?: "Đăng ký thất bại")
            }
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            try {
                val result = firebaseAuthManager.sendEmailVerification()
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        _registerState.value = LoginState.EmailVerificationSent
                    }
                    is FirebaseAuthResult.Error -> {
                        // Log error but don't show to user as registration was successful
                    }
                    is FirebaseAuthResult.Loading -> {
                        // Handle loading if needed
                    }
                }
            } catch (e: Exception) {
                // Log error but don't show to user as registration was successful
            }
        }
    }
} 