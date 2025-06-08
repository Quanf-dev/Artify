package com.example.imageaigen.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.imageaigen.data.repository.GeminiRepository
import kotlinx.coroutines.launch

class RemoveBackgroundViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository(application.applicationContext)
    private val _resultBitmap = MutableLiveData<Bitmap?>()
    val resultBitmap: LiveData<Bitmap?> = _resultBitmap
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun removeBackground(bitmap: Bitmap) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.editImage(bitmap)
            _resultBitmap.postValue(result.bitmap)
            _isLoading.postValue(false)
        }
    }
} 