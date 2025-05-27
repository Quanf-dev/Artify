package com.example.artify.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artify.R
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.FirebaseAuthResult
import com.example.firebaseauth.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupUsernameViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _setupState = MutableLiveData<SetupUsernameState>()
    val setupState: LiveData<SetupUsernameState> = _setupState

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    init {
        fetchCurrentUser()
    }

    private fun fetchCurrentUser(){
        viewModelScope.launch {
            val user = firebaseAuthManager.getCurrentUser()
            _currentUser.value = user
            if (user?.username != null){
                 _setupState.value = SetupUsernameState.UsernameAlreadySet(user.username!!)
            }
        }
    }

    fun saveUsername(username: String) {
        val uid = firebaseAuthManager.getCurrentUser()?.uid
        if (uid == null) {
            _setupState.value = SetupUsernameState.Error(context.getString(R.string.user_not_authenticated_error))
            return
        }

        if (username.length < 3) {
            _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_min_length))
            return
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_invalid_chars))
            return
        }

        viewModelScope.launch {
            _setupState.value = SetupUsernameState.Loading
            when (val checkResult = firebaseAuthManager.checkUsernameExists(username)) {
                is FirebaseAuthResult.Success -> {
                    if (checkResult.data) {
                        _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_taken))
                    } else {
                        // Username is available, proceed to save
                        when (val saveResult = firebaseAuthManager.saveUsername(uid, username)) {
                            is FirebaseAuthResult.Success -> {
                                _setupState.value = SetupUsernameState.Success(username)
                            }
                            is FirebaseAuthResult.Error -> {
                                _setupState.value = SetupUsernameState.Error(saveResult.exception.message ?: context.getString(R.string.username_error_generic_save))
                            }
                            else -> _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_generic_save)) // Or a more specific unexpected error string
                        }
                    }
                }
                is FirebaseAuthResult.Error -> {
                    _setupState.value = SetupUsernameState.Error(checkResult.exception.message ?: context.getString(R.string.username_error_generic_check))
                }
                 else -> _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_generic_check)) // Or a more specific unexpected error string
            }
        }
    }
}

sealed class SetupUsernameState {
    object Idle : SetupUsernameState()
    object Loading : SetupUsernameState()
    data class Success(val username: String) : SetupUsernameState()
    data class UsernameAlreadySet(val username: String) : SetupUsernameState()
    data class Error(val message: String) : SetupUsernameState()
} 