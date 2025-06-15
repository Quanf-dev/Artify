package com.example.imageaigen.ui.logo

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.imageaigen.data.repository.GeminiRepository
import kotlinx.coroutines.launch

class LogoMakerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository(application.applicationContext)
    private val _images = MutableLiveData<List<Bitmap>>()
    val images: LiveData<List<Bitmap>> = _images
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun generateLogo(prompt: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.generateAnimeImages(prompt)
            _images.postValue(result)
            _isLoading.postValue(false)
        }
    }
} 