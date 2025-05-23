package com.example.firebaseauth.viewmodel

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauth.FirebaseAuthResult
import com.example.firebaseauth.model.User
import com.example.firebaseauth.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<FirebaseAuthResult<User>>()
    val authState: LiveData<FirebaseAuthResult<User>> = _authState

    val currentUser = authRepository.getAuthStateFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            authRepository.getCurrentUser()
        )

    // Email & Password
    fun signInWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = FirebaseAuthResult.Loading
            _authState.value = authRepository.signInWithEmailAndPassword(email, password)
        }
    }

    fun createUserWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = FirebaseAuthResult.Loading
            _authState.value = authRepository.createUserWithEmailAndPassword(email, password)
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _authState.value = FirebaseAuthResult.Loading
            val result = authRepository.sendPasswordResetEmail(email)
            if (result is FirebaseAuthResult.Success) {
                // Xử lý thành công
            } else if (result is FirebaseAuthResult.Error) {
                _authState.value = FirebaseAuthResult.error(result.exception)
            }
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            val result = authRepository.sendEmailVerification()
            if (result is FirebaseAuthResult.Error) {
                _authState.value = FirebaseAuthResult.error(result.exception)
            }
        }
    }

    // Google Sign-In
    fun getGoogleSignInIntent(): Intent {
        return authRepository.getGoogleSignInIntent()
    }

    fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                viewModelScope.launch {
                    _authState.value = FirebaseAuthResult.Loading
                    _authState.value = authRepository.signInWithGoogle(credential)
                }
            }
        } catch (e: ApiException) {
            _authState.value = FirebaseAuthResult.error(e)
        }
    }

    // Phone Authentication
    fun sendPhoneVerificationCode(
        phoneNumber: String,
        onCodeSent: (String) -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = FirebaseAuthResult.Loading
            authRepository.sendPhoneVerificationCode(
                phoneNumber = phoneNumber,
                onCodeSent = onCodeSent,
                onVerificationFailed = { exception ->
                    _authState.value = FirebaseAuthResult.error(exception)
                }
            )
        }
    }

    fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        viewModelScope.launch {
            _authState.value = FirebaseAuthResult.Loading
            _authState.value = authRepository.verifyPhoneNumberWithCode(verificationId, code)
        }
    }

    // Common
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun isUserSignedIn(): Boolean {
        return authRepository.isUserSignedIn()
    }
}