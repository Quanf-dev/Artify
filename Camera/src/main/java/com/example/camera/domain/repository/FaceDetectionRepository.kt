package com.example.camera.domain.repository

import android.graphics.Bitmap
import com.example.camera.domain.model.DetectedFace
import com.example.camera.domain.model.FaceMask
import kotlinx.coroutines.flow.Flow

interface FaceDetectionRepository {
    
    fun detectFaces(bitmap: Bitmap): Flow<List<DetectedFace>>
    
    fun getAvailableMasks(): List<FaceMask>
    
    suspend fun applyMaskToFace(
        originalBitmap: Bitmap,
        detectedFaces: List<DetectedFace>,
        selectedMask: FaceMask
    ): Bitmap
    
    fun startRealTimeFaceDetection(): Flow<List<DetectedFace>>
    
    fun stopRealTimeFaceDetection()
    
    fun isDetectionActive(): Boolean
} 