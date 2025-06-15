package com.example.artify.ui.phone

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import com.example.firebaseauth.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PhoneLoginState {
    object Idle : PhoneLoginState()
    object Loading : PhoneLoginState()
    data class Success(val user: User) : PhoneLoginState()
    data class Error(val message: String) : PhoneLoginState()
    object PhoneVerificationCodeSent : PhoneLoginState()
}

@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {

    private val _phoneLoginState = MutableLiveData<PhoneLoginState>(PhoneLoginState.Idle)
    val phoneLoginState: LiveData<PhoneLoginState> = _phoneLoginState

    var verificationId: String? = null
        private set

    fun loginWithPhone(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _phoneLoginState.value = PhoneLoginState.Loading
            try {
                firebaseAuthManager.sendPhoneVerificationCode(
                    phoneNumber = phoneNumber,
                    activity = activity,
                    onCodeSent = { sentVerificationId ->
                        this@PhoneViewModel.verificationId = sentVerificationId
                        _phoneLoginState.value = PhoneLoginState.PhoneVerificationCodeSent
                    },
                    onVerificationFailed = { exception ->
                        _phoneLoginState.value = PhoneLoginState.Error(exception.message ?: "Gửi mã xác thực thất bại")
                    }
                )
            } catch (e: Exception) {
                _phoneLoginState.value = PhoneLoginState.Error(e.message ?: "Gửi mã xác thực thất bại")
            }
        }
    }

    fun verifyPhoneNumberWithCode(code: String) {
        viewModelScope.launch {
            val currentVerificationId = verificationId
            if (currentVerificationId == null) {
                _phoneLoginState.value = PhoneLoginState.Error("Verification ID is missing.")
                return@launch
            }
            _phoneLoginState.value = PhoneLoginState.Loading
            try {
                val result = firebaseAuthManager.verifyPhoneNumberWithCode(currentVerificationId, code)
                when (result) {
                    is FirebaseAuthResult.Success -> {
                        // Fetch user with username after successful phone verification
                        val currentUser = firebaseAuthManager.getCurrentUserWithUsername()
                        if (currentUser != null) {
                            _phoneLoginState.value = PhoneLoginState.Success(currentUser)
                        } else {
                            _phoneLoginState.value = PhoneLoginState.Success(result.data)
                        }
                    }
                    is FirebaseAuthResult.Error -> {
                        _phoneLoginState.value = PhoneLoginState.Error(result.exception.message ?: "Xác thực mã thất bại")
                    }
                    is FirebaseAuthResult.Loading -> {
                         // Already in loading state, or let firebaseAuthManager handle this if it emits Loading
                        _phoneLoginState.value = PhoneLoginState.Loading
                    }
                }
            } catch (e: Exception) {
                _phoneLoginState.value = PhoneLoginState.Error(e.message ?: "Xác thực mã thất bại")
            }
        }
    }
}