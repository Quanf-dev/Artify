package com.example.camera.domain.usecase

import com.example.camera.domain.model.FaceMask
import com.example.camera.domain.repository.FaceDetectionRepository
import javax.inject.Inject

class GetFaceMasksUseCase @Inject constructor(
    private val faceDetectionRepository: FaceDetectionRepository
) {
    
    operator fun invoke(): List<FaceMask> {
        return faceDetectionRepository.getAvailableMasks()
    }
} 