package com.example.camera.di

import com.example.camera.data.repository.CameraRepositoryImpl
import com.example.camera.data.repository.FaceDetectionRepositoryImpl
import com.example.camera.domain.repository.CameraRepository
import com.example.camera.domain.repository.FaceDetectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        cameraRepositoryImpl: CameraRepositoryImpl
    ): CameraRepository

    @Binds
    @Singleton
    abstract fun bindFaceDetectionRepository(
        faceDetectionRepositoryImpl: FaceDetectionRepositoryImpl
    ): FaceDetectionRepository
} 