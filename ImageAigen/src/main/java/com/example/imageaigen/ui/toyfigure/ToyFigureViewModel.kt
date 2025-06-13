package com.example.imageaigen.ui.toyfigure

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.imageaigen.data.repository.GeminiRepository
import kotlinx.coroutines.launch

class ToyFigureViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository(application.applicationContext)
    private val _images = MutableLiveData<List<Bitmap>>()
    val images: LiveData<List<Bitmap>> = _images
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun generateToyFigure(image: Bitmap, prompt: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val response = repository.startChatAndEditImage(image, prompt)
            if (response.isError || response.bitmap == null) {
                _images.postValue(emptyList())
            } else {
                _images.postValue(listOf(response.bitmap))
            }
            _isLoading.postValue(false)
        }
    }
} 