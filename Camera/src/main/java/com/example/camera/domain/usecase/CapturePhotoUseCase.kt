package com.example.camera.domain.usecase

import com.example.camera.domain.model.CameraEvent
import com.example.camera.domain.model.CameraSettings
import com.example.camera.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CapturePhotoUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    
    operator fun invoke(settings: CameraSettings): Flow<CameraEvent> {
        return cameraRepository.capturePhoto(settings)
    }
} 