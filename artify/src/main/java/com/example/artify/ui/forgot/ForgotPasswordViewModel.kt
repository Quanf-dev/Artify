    package com.example.artify.ui.forgot

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artify.R
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    object EmailSentSuccess : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _forgotPasswordState = MutableLiveData<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: LiveData<ForgotPasswordState> = _forgotPasswordState

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState.Loading
            if (email.isEmpty()) {
                _forgotPasswordState.value = ForgotPasswordState.Error(context.getString(R.string.error_email_empty_forgot))
                return@launch
            }
            // Basic email validation (can be improved with regex)
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _forgotPasswordState.value = ForgotPasswordState.Error(context.getString(R.string.error_email_invalid_format))
                return@launch
            }

            when (val result = firebaseAuthManager.sendPasswordResetEmail(email)) {
                is FirebaseAuthResult.Success -> {
                    _forgotPasswordState.value = ForgotPasswordState.EmailSentSuccess
                }
                is FirebaseAuthResult.Error -> {
                    _forgotPasswordState.value = ForgotPasswordState.Error(result.exception.message ?: context.getString(R.string.error_reset_email_failed))
                }
                else -> {
                    // Should not happen for sendPasswordResetEmail if it's not returning Loading state explicitly
                    _forgotPasswordState.value = ForgotPasswordState.Error(context.getString(R.string.error_unexpected_occurred))
                }
            }
        }
    }

} 