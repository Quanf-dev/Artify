package com.example.camera.ui.camera

import com.example.camera.domain.model.*

data class CameraUiState(
    val isLoading: Boolean = false,
    val cameraSettings: CameraSettings = CameraSettings(),
    val availableMasks: List<FaceMask> = emptyList(),
    val selectedMask: FaceMask? = null,
    val detectedFaces: List<DetectedFace> = emptyList(),
    val isTimerActive: Boolean = false,
    val timerCountdown: Int = 0,
    val capturedPhoto: CapturedPhoto? = null,
    val showPreview: Boolean = false,
    val errorMessage: String? = null,
    val permissionsGranted: Boolean = false,
    val isFaceDetectionEnabled: Boolean = false,
    val zoomRange: Pair<Float, Float> = Pair(1.0f, 1.0f),
    val cameraPreviewWidth: Int = 0,
    val cameraPreviewHeight: Int = 0
)

sealed class CameraUiEvent {
    object TakePhoto : CameraUiEvent()
    object SwitchCamera : CameraUiEvent()
    object ToggleFlash : CameraUiEvent()
    object OpenGallery : CameraUiEvent()
    object DismissPreview : CameraUiEvent()
    object SavePhoto : CameraUiEvent()
    object EditPhoto : CameraUiEvent()
    object RetakePhoto : CameraUiEvent()
    object ToggleFaceDetection : CameraUiEvent()
    data class SetZoom(val zoom: Float) : CameraUiEvent()
    data class SetBrightness(val brightness: Float) : CameraUiEvent()
    data class SetAspectRatio(val aspectRatio: AspectRatio) : CameraUiEvent()
    data class SetTimer(val seconds: Int) : CameraUiEvent()
    data class SelectFilter(val filter: FilterType) : CameraUiEvent()
    data class SelectMask(val mask: FaceMask?) : CameraUiEvent()
    data class PermissionResult(val granted: Boolean) : CameraUiEvent()
} 