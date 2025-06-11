package com.example.camera.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class CapturedPhoto(
    val id: String,
    val uri: Uri,
    val fileName: String,
    val filePath: String,
    val dateTaken: Date,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val cameraSettings: CameraSettings,
    val appliedMask: String? = null
) : Parcelable

data class PhotoEditOptions(
    val brightness: Float = 0.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val hue: Float = 0.0f,
    val rotation: Float = 0.0f,
    val cropRect: android.graphics.RectF? = null,
    val appliedFilter: FilterType = FilterType.NONE
)

sealed class CameraEvent {
    object PhotoCaptured : CameraEvent()
    object VideoRecordingStarted : CameraEvent()
    object VideoRecordingStopped : CameraEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : CameraEvent()
    object PermissionDenied : CameraEvent()
    object CameraNotAvailable : CameraEvent()
} 