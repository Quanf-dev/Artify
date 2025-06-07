package com.example.imageaigen.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.data.repository.GeminiRepository
import kotlinx.coroutines.launch

class GeminiViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GeminiRepository(application.applicationContext)
    
    private val _imageGenerationResult = MutableLiveData<GeminiResponse>()
    val imageGenerationResult: LiveData<GeminiResponse> = _imageGenerationResult
    
    private val _imageEditResult = MutableLiveData<GeminiResponse>()
    val imageEditResult: LiveData<GeminiResponse> = _imageEditResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Generate an image from a text prompt
     */
    fun generateImage(prompt: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.generateImageFromText(prompt)
            _imageGenerationResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
    
    /**
     * Edit an existing image with text instructions
     */
    fun editImage(image: Bitmap, prompt: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.editImage(image, prompt)
            _imageEditResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
    
    /**
     * Start a chat session for iterative image editing
     */
    fun startChatAndEditImage(image: Bitmap, prompt: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.startChatAndEditImage(image, prompt)
            _imageEditResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
} 