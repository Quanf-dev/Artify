package com.example.artify.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artify.R
import com.example.artify.data.source.DefaultAvatars
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

    private val _availableAvatars = MutableLiveData<List<String>>()
    val availableAvatars: LiveData<List<String>> = _availableAvatars

    init {
        fetchCurrentUser()
        loadAvailableAvatars()
    }

    private fun fetchCurrentUser(){
        viewModelScope.launch {
            val user: User? = firebaseAuthManager.getCurrentUser()
            _currentUser.value = user
            // We navigate to main if username is already set, avatar can be updated later if needed
            if (user?.username != null){
                 _setupState.value = SetupUsernameState.UsernameAlreadySet(user.username!!)
            }
        }
    }

    private fun loadAvailableAvatars() {
        // For now, we load a static list. This could be an async call in the future.
        _availableAvatars.value = DefaultAvatars.avatarUrls
    }

    // Old saveUsername function - can be removed or kept if there's a use case for saving only username
    /*
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
                            else -> _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_generic_save))
                        }
                    }
                }
                is FirebaseAuthResult.Error -> {
                    _setupState.value = SetupUsernameState.Error(checkResult.exception.message ?: context.getString(R.string.username_error_generic_check))
                }
                 else -> _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_generic_check))
            }
        }
    }
    */

    fun saveUsernameAndAvatar(username: String, avatarUrl: String?) {
        val uid: String? = firebaseAuthManager.getCurrentUser()?.uid
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
        // Avatar URL can be null if user doesn't select one, or if you make it optional
        // If avatar is mandatory, add a check here
        if (avatarUrl == null){
            _setupState.value = SetupUsernameState.Error(context.getString(R.string.avatar_not_selected_error)) // Add this string to your strings.xml
            return
        }

        viewModelScope.launch {
            _setupState.value = SetupUsernameState.Loading
            // First, check if username exists (if it's a new username being set)
            // If username is already set and we are just updating avatar, this check might be different
            val currentUser: User? = _currentUser.value
            val isUpdatingUsername: Boolean = currentUser?.username == null || currentUser.username != username

            if (isUpdatingUsername) {
                when (val checkResult = firebaseAuthManager.checkUsernameExists(username)) {
                    is FirebaseAuthResult.Success -> {
                        if (checkResult.data) {
                            _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_taken))
                            return@launch // Exit coroutine early
                        }
                        // Username is available, proceed to save username and avatar
                        saveUserDetails(uid, username, avatarUrl)
                    }
                    is FirebaseAuthResult.Error -> {
                        _setupState.value = SetupUsernameState.Error(checkResult.exception.message ?: context.getString(R.string.username_error_generic_check))
                        return@launch
                    }
                    else -> {
                        _setupState.value = SetupUsernameState.Error(context.getString(R.string.username_error_generic_check))
                        return@launch
                    }
                }
            } else {
                // Username is not changing, just update avatar (and potentially other details if username is also being updated)
                 saveUserDetails(uid, username, avatarUrl)
            }
        }
    }

    private suspend fun saveUserDetails(uid: String, username: String, avatarUrl: String) {
        when (val saveResult = firebaseAuthManager.saveUsernameAndPhotoUrl(uid, username, avatarUrl)) {
            is FirebaseAuthResult.Success -> {
                _setupState.value = SetupUsernameState.Success(username) // Username is the primary identifier here for success message
                 // Fetch the updated user details to reflect new avatar in UI if needed immediately
                val updatedUser = firebaseAuthManager.getCurrentUser()
                _currentUser.value = updatedUser
            }
            is FirebaseAuthResult.Error -> {
                _setupState.value = SetupUsernameState.Error(saveResult.exception.message ?: context.getString(R.string.user_details_save_error)) // Add this string
            }
            else -> _setupState.value = SetupUsernameState.Error(context.getString(R.string.user_details_save_error)) // Add this string
        }
    }
}

sealed class SetupUsernameState {
    object Idle : SetupUsernameState()
    object Loading : SetupUsernameState()
    data class Success(val username: String) : SetupUsernameState() // photoUrl could also be part of success if needed by UI immediately
    data class UsernameAlreadySet(val username: String) : SetupUsernameState()
    data class Error(val message: String) : SetupUsernameState()
} 