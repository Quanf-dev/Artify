package com.example.imageaigen.ui.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PreviewImageViewModel : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _images = MutableLiveData<List<android.graphics.Bitmap>>()
    val images: LiveData<List<android.graphics.Bitmap>> = _images

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setImages(bitmaps: List<android.graphics.Bitmap>) {
        _images.value = bitmaps
    }
} 