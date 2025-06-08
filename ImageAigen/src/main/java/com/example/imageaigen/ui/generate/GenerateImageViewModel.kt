package com.example.imageaigen.ui.generate

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.data.repository.GeminiRepository
import kotlinx.coroutines.launch

class GenerateImageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository(application.applicationContext)
    private val _imageGenerationResult = MutableLiveData<GeminiResponse>()
    val imageGenerationResult: LiveData<GeminiResponse> = _imageGenerationResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun generateImage(prompt: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.generateImageFromText(prompt)
            _imageGenerationResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
} 