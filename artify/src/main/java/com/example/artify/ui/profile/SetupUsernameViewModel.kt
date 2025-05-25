package com.example.artify.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupUsernameViewModel @Inject constructor(
) : ViewModel() {

    private val _setupState = MutableLiveData<SetupUsernameState>()
    val setupState: LiveData<SetupUsernameState> = _setupState

    fun saveUsername(username: String) {
        viewModelScope.launch {
            _setupState.value = SetupUsernameState.Loading
            try {
                // Ở đây bạn có thể lưu username vào Firebase Firestore hoặc Realtime Database
                // Hiện tại chỉ simulate thành công
                kotlinx.coroutines.delay(1000) // Simulate network call
                _setupState.value = SetupUsernameState.Success
            } catch (e: Exception) {
                _setupState.value = SetupUsernameState.Error(e.message ?: "Lưu tên người dùng thất bại")
            }
        }
    }
}

sealed class SetupUsernameState {
    object Loading : SetupUsernameState()
    object Success : SetupUsernameState()
    data class Error(val message: String) : SetupUsernameState()
} 