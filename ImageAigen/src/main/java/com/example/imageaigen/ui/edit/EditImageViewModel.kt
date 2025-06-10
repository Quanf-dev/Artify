package com.example.imageaigen.ui.edit

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.data.repository.GeminiRepository
import kotlinx.coroutines.launch

class EditImageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository(application.applicationContext)
    private val _imageEditResult = MutableLiveData<GeminiResponse>()
    val imageEditResult: LiveData<GeminiResponse> = _imageEditResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun editImage(image: Bitmap, prompt: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.startChatAndEditImage(image, prompt)
                _imageEditResult.postValue(result)
            } catch (e: Exception) {
                _imageEditResult.postValue(
                    GeminiResponse(
                        bitmap = null,
                        text = null,
                        isError = true,
                        errorMessage = e.message ?: "Failed to edit image"
                    )
                )
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}