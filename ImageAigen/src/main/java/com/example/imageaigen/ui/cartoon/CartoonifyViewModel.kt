package com.example.imageaigen.ui.cartoon

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.data.repository.GeminiRepository
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.launch

class CartoonifyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository(application.applicationContext)
    private val _result = MutableLiveData<GeminiResponse>()
    val result: LiveData<GeminiResponse> = _result
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    @OptIn(PublicPreviewAPI::class)
    fun cartoonifyImage(image: Bitmap) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.CartoonImage(
                image
            )
            _result.postValue(result)
            _isLoading.postValue(false)
        }
    }

}