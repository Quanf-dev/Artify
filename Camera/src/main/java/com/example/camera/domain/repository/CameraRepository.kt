package com.example.camera.domain.repository

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import com.example.camera.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    
    fun initializeCamera(previewView: PreviewView, lifecycleOwner: androidx.lifecycle.LifecycleOwner)
    
    fun capturePhoto(settings: CameraSettings): Flow<CameraEvent>
    
    fun switchCamera(isBackCamera: Boolean)
    
    fun setZoomLevel(zoomLevel: Float)
    
    fun setBrightness(brightness: Float)
    
    fun setFlashMode(flashMode: FlashMode)
    
    fun applyFilter(filter: FilterType)
    
    fun setAspectRatio(aspectRatio: AspectRatio)
    
    fun getAvailableZoomRange(): Pair<Float, Float>
    
    fun isCameraAvailable(): Boolean
    
    fun releaseCamera()
    
    suspend fun savePhoto(bitmap: Bitmap, settings: CameraSettings, appliedMask: String?): CapturedPhoto
    
    suspend fun getRecentPhotos(limit: Int = 10): List<CapturedPhoto>
} 