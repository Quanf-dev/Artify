package com.example.camera.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.view.PreviewView
import com.example.camera.domain.model.*
import com.example.camera.domain.repository.CameraRepository
import com.example.camera.domain.repository.FaceDetectionRepository
import com.example.camera.domain.usecase.CapturePhotoUseCase
import com.example.camera.domain.usecase.GetFaceMasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val faceDetectionRepository: FaceDetectionRepository,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val getFaceMasksUseCase: GetFaceMasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var faceDetectionJob: Job? = null

    init {
        loadAvailableMasks()
        initializeZoomRange()
    }

    fun onEvent(event: CameraUiEvent) {
        when (event) {
            is CameraUiEvent.TakePhoto -> takePhoto()
            is CameraUiEvent.SwitchCamera -> switchCamera()
            is CameraUiEvent.ToggleFlash -> toggleFlash()
            is CameraUiEvent.OpenGallery -> openGallery()
            is CameraUiEvent.DismissPreview -> dismissPreview()
            is CameraUiEvent.SavePhoto -> savePhoto()
            is CameraUiEvent.EditPhoto -> editPhoto()
            is CameraUiEvent.RetakePhoto -> retakePhoto()
            is CameraUiEvent.ToggleFaceDetection -> toggleFaceDetection()
            is CameraUiEvent.SetZoom -> setZoom(event.zoom)
            is CameraUiEvent.SetBrightness -> setBrightness(event.brightness)
            is CameraUiEvent.SetAspectRatio -> setAspectRatio(event.aspectRatio)
            is CameraUiEvent.SetTimer -> setTimer(event.seconds)
            is CameraUiEvent.SelectFilter -> selectFilter(event.filter)
            is CameraUiEvent.SelectMask -> selectMask(event.mask)
            is CameraUiEvent.PermissionResult -> handlePermissionResult(event.granted)
        }
    }

    private fun takePhoto() {
        val currentState = _uiState.value
        if (currentState.isTimerActive || currentState.isLoading) return

        if (currentState.cameraSettings.timerSeconds > 0) {
            startTimer(currentState.cameraSettings.timerSeconds)
        } else {
            capturePhoto()
        }
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.update { it.copy(isTimerActive = true, timerCountdown = seconds) }
            
            for (countdown in seconds downTo 1) {
                _uiState.update { it.copy(timerCountdown = countdown) }
                delay(1000)
            }
            
            _uiState.update { it.copy(isTimerActive = false, timerCountdown = 0) }
            capturePhoto()
        }
    }

    private fun capturePhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            capturePhotoUseCase(_uiState.value.cameraSettings)
                .catch { throwable ->
                    Timber.e(throwable, "Photo capture failed")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to capture photo: ${throwable.message}"
                        )
                    }
                }
                .collect { event ->
                    when (event) {
                        is CameraEvent.PhotoCaptured -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    showPreview = true
                                )
                            }
                        }
                        is CameraEvent.Error -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = event.message
                                )
                            }
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun switchCamera() {
        val currentSettings = _uiState.value.cameraSettings
        val newSettings = currentSettings.copy(isBackCamera = !currentSettings.isBackCamera)
        
        cameraRepository.switchCamera(newSettings.isBackCamera)
        _uiState.update { it.copy(cameraSettings = newSettings) }
    }

    private fun toggleFlash() {
        val currentSettings = _uiState.value.cameraSettings
        val newFlashMode = when (currentSettings.flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        
        val newSettings = currentSettings.copy(flashMode = newFlashMode)
        cameraRepository.setFlashMode(newFlashMode)
        _uiState.update { it.copy(cameraSettings = newSettings) }
    }

    private fun setZoom(zoom: Float) {
        val currentSettings = _uiState.value.cameraSettings
        val newSettings = currentSettings.copy(zoomLevel = zoom)
        
        cameraRepository.setZoomLevel(zoom)
        _uiState.update { it.copy(cameraSettings = newSettings) }
    }

    private fun setBrightness(brightness: Float) {
        val currentSettings = _uiState.value.cameraSettings
        val newSettings = currentSettings.copy(brightnessLevel = brightness)
        
        cameraRepository.setBrightness(brightness)
        _uiState.update { it.copy(cameraSettings = newSettings) }
    }

    private fun setAspectRatio(aspectRatio: AspectRatio) {
        val currentSettings = _uiState.value.cameraSettings
        val newSettings = currentSettings.copy(aspectRatio = aspectRatio)
        
        cameraRepository.setAspectRatio(aspectRatio)
        _uiState.update { it.copy(cameraSettings = newSettings) }
        Timber.d("Aspect ratio updated to: ${aspectRatio.ratio}")
    }

    private fun setTimer(seconds: Int) {
        val currentSettings = _uiState.value.cameraSettings
        val newSettings = currentSettings.copy(timerSeconds = seconds)
        _uiState.update { it.copy(cameraSettings = newSettings) }
    }

    private fun selectFilter(filter: FilterType) {
        val currentSettings = _uiState.value.cameraSettings
        val newSettings = currentSettings.copy(
            currentFilter = filter,
            selectedFilter = filter
        )
        
        cameraRepository.applyFilter(filter)
        _uiState.update { it.copy(cameraSettings = newSettings) }
    }

    private fun selectMask(mask: FaceMask?) {
        _uiState.update { it.copy(selectedMask = mask) }
    }

    private fun toggleFaceDetection() {
        val isEnabled = !_uiState.value.isFaceDetectionEnabled
        _uiState.update { it.copy(isFaceDetectionEnabled = isEnabled) }
        
        if (isEnabled) {
            startFaceDetection()
        } else {
            stopFaceDetection()
        }
    }

    private fun startFaceDetection() {
        faceDetectionJob?.cancel()
        faceDetectionJob = viewModelScope.launch {
            faceDetectionRepository.startRealTimeFaceDetection()
                .catch { throwable ->
                    Timber.e(throwable, "Face detection failed")
                }
                .collect { detectedFaces ->
                    _uiState.update { it.copy(detectedFaces = detectedFaces) }
                }
        }
    }

    private fun stopFaceDetection() {
        faceDetectionJob?.cancel()
        faceDetectionRepository.stopRealTimeFaceDetection()
        _uiState.update { it.copy(detectedFaces = emptyList()) }
    }

    private fun openGallery() {
        // Gallery opening logic would be handled by the Fragment/Activity
    }

    private fun dismissPreview() {
        _uiState.update { 
            it.copy(
                showPreview = false,
                capturedPhoto = null
            )
        }
    }

    private fun savePhoto() {
        dismissPreview()
    }

    private fun editPhoto() {
        // Edit photo logic would navigate to photo editor
    }

    private fun retakePhoto() {
        dismissPreview()
    }

    private fun handlePermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionsGranted = granted) }
        if (!granted) {
            _uiState.update { 
                it.copy(errorMessage = "Camera permissions are required to use this feature")
            }
        }
    }

    private fun loadAvailableMasks() {
        viewModelScope.launch {
            try {
                val masks = getFaceMasksUseCase()
                _uiState.update { it.copy(availableMasks = masks) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load masks")
            }
        }
    }

    private fun initializeZoomRange() {
        val zoomRange = cameraRepository.getAvailableZoomRange()
        _uiState.update { it.copy(zoomRange = zoomRange) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun initializeCamera(previewView: PreviewView, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        cameraRepository.initializeCamera(previewView, lifecycleOwner)
        
        // Set up callbacks to connect camera frames with face detection
        if (cameraRepository is com.example.camera.data.repository.CameraRepositoryImpl) {
            // Face detection callback
            cameraRepository.setFaceDetectionCallback { detectedFaces ->
                if (faceDetectionRepository is com.example.camera.data.repository.FaceDetectionRepositoryImpl) {
                    faceDetectionRepository.updateDetectedFaces(detectedFaces)
                }
            }
            
            // Preview dimensions callback for coordinate mapping
            cameraRepository.setPreviewDimensionsCallback { width, height ->
                _uiState.update { 
                    it.copy(
                        cameraPreviewWidth = width,
                        cameraPreviewHeight = height
                    ) 
                }
                Timber.d("Updated preview dimensions in ViewModel: ${width}x${height}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        faceDetectionJob?.cancel()
        
        // Clear face detection callback
        if (cameraRepository is com.example.camera.data.repository.CameraRepositoryImpl) {
            cameraRepository.clearFaceDetectionCallback()
        }
        
        cameraRepository.releaseCamera()
    }
} 