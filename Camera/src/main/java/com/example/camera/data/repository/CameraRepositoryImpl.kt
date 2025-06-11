package com.example.camera.data.repository

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.MediaStore
import android.util.SparseIntArray
import android.view.Surface
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.camera.domain.model.*
import com.example.camera.domain.repository.CameraRepository
import com.example.camera.domain.model.AspectRatio as DomainAspectRatio
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraRepository {

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // Face detection
    private val faceDetectorOptions = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()
    
    private val faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(faceDetectorOptions)
    
    // Used for rotation compensation
    private val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    
    private var currentCameraId: String? = null
    private var faceDetectionCallback: ((List<DetectedFace>) -> Unit)? = null
    private var previewDimensionsCallback: ((Int, Int) -> Unit)? = null
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    override fun initializeCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        Timber.d("Starting camera initialization")
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                Timber.d("Camera provider obtained")
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(previewView, lifecycleOwner)
                Timber.d("Camera use cases bound successfully")
            } catch (exc: Exception) {
                Timber.e(exc, "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private var currentAspectRatio: DomainAspectRatio = DomainAspectRatio.RATIO_4_3

    private fun bindCameraUseCases(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        Timber.d("Binding camera use cases with lens facing: $lensFacing")
        val cameraProvider = this.cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
            
        // Get camera ID for rotation compensation
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        currentCameraId = cameraManager.cameraIdList.firstOrNull { 
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val lensFacingChar = characteristics.get(CameraCharacteristics.LENS_FACING)
            lensFacingChar == when(lensFacing) {
                CameraSelector.LENS_FACING_BACK -> CameraCharacteristics.LENS_FACING_BACK
                CameraSelector.LENS_FACING_FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                else -> null
            }
        }

        // Convert DomainAspectRatio to CameraX AspectRatio
        val aspectRatio = when (currentAspectRatio) {
            DomainAspectRatio.RATIO_4_3 -> AspectRatio.RATIO_4_3
            DomainAspectRatio.RATIO_16_9 -> AspectRatio.RATIO_16_9
            DomainAspectRatio.RATIO_1_1 -> AspectRatio.RATIO_4_3 // CameraX doesn't have 1:1, use 4:3 and crop in UI
            DomainAspectRatio.RATIO_3_4 -> AspectRatio.RATIO_4_3 // Use 4:3 and rotate/crop in UI
            DomainAspectRatio.RATIO_FULL -> AspectRatio.RATIO_16_9 // Use 16:9 for full screen
        }

        // Preview use case
        preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // Image capture use case
        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()

        // Image analysis use case for face detection
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    detectFacesInFrame(imageProxy)
                }
            }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            
            Timber.d("Camera bound successfully")
        } catch (exc: Exception) {
            Timber.e(exc, "Failed to bind camera use cases")
        }
    }

    override fun capturePhoto(settings: CameraSettings): Flow<CameraEvent> = flow {
        try {
            val imageCapture = this@CameraRepositoryImpl.imageCapture ?: throw IllegalStateException("ImageCapture not initialized")
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${timeStamp}")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraXApp")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Timber.e(exception, "Photo capture failed")
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Timber.d("Photo captured successfully")
                    }
                }
            )
            
            emit(CameraEvent.PhotoCaptured)
        } catch (exc: Exception) {
            emit(CameraEvent.Error("Failed to capture photo", exc))
        }
    }

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    override fun switchCamera(isBackCamera: Boolean) {
        lensFacing = if (isBackCamera) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        
        // Rebind camera with new facing if camera is already initialized
        currentLifecycleOwner?.let { lifecycleOwner ->
            currentPreviewView?.let { previewView ->
                bindCameraUseCases(previewView, lifecycleOwner)
            }
        }
    }

    override fun setZoomLevel(zoomLevel: Float) {
        camera?.cameraControl?.setZoomRatio(zoomLevel)
    }

    override fun setBrightness(brightness: Float) {
        val exposureValue = (brightness - 0.5f) * 4.0f
        camera?.cameraControl?.setExposureCompensationIndex(exposureValue.toInt())
    }

    override fun setFlashMode(flashMode: FlashMode) {
        val flashModeValue = when (flashMode) {
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        }
        imageCapture?.flashMode = flashModeValue
    }

    override fun applyFilter(filter: FilterType) {
        // Filter implementation would be handled in the UI layer
        // This is a placeholder for filter application logic
    }

    override fun setAspectRatio(aspectRatio: DomainAspectRatio) {
        currentAspectRatio = aspectRatio
        Timber.d("Aspect ratio changed to: ${aspectRatio.ratio}")
        
        // Rebind camera with new aspect ratio if camera is initialized
        currentLifecycleOwner?.let { lifecycleOwner ->
            currentPreviewView?.let { previewView ->
                bindCameraUseCases(previewView, lifecycleOwner)
            }
        }
    }

    override fun getAvailableZoomRange(): Pair<Float, Float> {
        val zoomState = camera?.cameraInfo?.zoomState?.value
        return Pair(
            zoomState?.minZoomRatio ?: 1.0f,
            zoomState?.maxZoomRatio ?: 1.0f
        )
    }

    override fun isCameraAvailable(): Boolean {
        return cameraProvider != null && camera != null
    }

    override fun releaseCamera() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    override suspend fun savePhoto(bitmap: Bitmap, settings: CameraSettings, appliedMask: String?): CapturedPhoto {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_${timeStamp}.jpg"
        val file = File(context.getExternalFilesDir(null), "CameraXApp/$fileName")
        
        file.parentFile?.mkdirs()
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        return CapturedPhoto(
            id = UUID.randomUUID().toString(),
            uri = android.net.Uri.fromFile(file),
            fileName = fileName,
            filePath = file.absolutePath,
            dateTaken = Date(),
            width = bitmap.width,
            height = bitmap.height,
            fileSize = file.length(),
            cameraSettings = settings,
            appliedMask = appliedMask
        )
    }

    override suspend fun getRecentPhotos(limit: Int): List<CapturedPhoto> {
        // Implementation for retrieving recent photos from storage
        // This would query the MediaStore or local file system
        return emptyList()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun detectFacesInFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Store actual image dimensions for coordinate mapping
            val newWidth = imageProxy.width
            val newHeight = imageProxy.height
            
            if (previewWidth != newWidth || previewHeight != newHeight) {
                previewWidth = newWidth
                previewHeight = newHeight
                previewDimensionsCallback?.invoke(previewWidth, previewHeight)
                Timber.d("Camera frame dimensions updated: ${previewWidth}x${previewHeight}")
            }
            
            try {
                // Create FirebaseVisionImage with proper rotation compensation
                val imageRotationDegrees = imageProxy.imageInfo.rotationDegrees
                Timber.d("Image rotation from imageProxy: $imageRotationDegrees")
                
                val rotation = getRotationCompensation(imageRotationDegrees)
                Timber.d("Using Firebase rotation value: $rotation")
                
                val image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation)
                Timber.d("Created FirebaseVisionImage from media image: ${image.bitmap.width}x${image.bitmap.height}")
                
                // Log memory usage before detection
                val runtime = Runtime.getRuntime()
                val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
                Timber.d("Memory before face detection: $usedMemoryMB MB used")
                
                faceDetector.detectInImage(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isEmpty()) {
                            Timber.d("No faces detected in the image")
                        } else {
                            Timber.d("Firebase detected ${faces.size} faces. First face bounds: ${faces.firstOrNull()?.boundingBox}")
                            
                            // Check if landmarks exist
                            faces.firstOrNull()?.let { firstFace ->
                                val hasLeftEye = firstFace.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE) != null
                                val hasRightEye = firstFace.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE) != null
                                val hasNose = firstFace.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE) != null
                                val hasMouth = firstFace.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null
                                Timber.d("Face landmarks - left eye: $hasLeftEye, right eye: $hasRightEye, nose: $hasNose, mouth: $hasMouth")
                            }
                        }
                        
                        val detectedFaces = faces.mapIndexed { index, face ->
                            val faceId = face.trackingId.takeIf { it != FirebaseVisionFace.INVALID_ID } ?: index
                            Timber.d("Processing face #$faceId, tracking ID: ${face.trackingId}, bounds: ${face.boundingBox}")
                            
                            DetectedFace(
                                id = faceId,
                                boundingBox = face.boundingBox,
                                rotationY = face.headEulerAngleY,
                                rotationZ = face.headEulerAngleZ,
                                leftEyePosition = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)?.position?.let {
                                    PointF(it.x, it.y)
                                },
                                rightEyePosition = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)?.position?.let {
                                    PointF(it.x, it.y)
                                },
                                nosePosition = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE)?.position?.let {
                                    PointF(it.x, it.y)
                                },
                                mouthPosition = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM)?.position?.let {
                                    PointF(it.x, it.y)
                                }
                            )
                        }
                        
                        Timber.d("Detected ${detectedFaces.size} faces in frame (${previewWidth}x${previewHeight})")
                        faceDetectionCallback?.invoke(detectedFaces)
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Face detection failed")
                        faceDetectionCallback?.invoke(emptyList())
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error creating FirebaseVisionImage")
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
    
    /**
     * Get the rotation compensation according to Firebase ML Kit docs
     * This properly handles device orientation and camera sensor orientation
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun getRotationCompensation(rotationDegrees: Int): Int {
        val activity = currentLifecycleOwner?.let {
            when (it) {
                is Activity -> it
                is Fragment -> it.activity
                else -> null
            }
        }
        
        // Simple conversion when we don't have activity context
        if (activity == null || currentCameraId == null) {
            return when (rotationDegrees) {
                0 -> FirebaseVisionImageMetadata.ROTATION_0
                90 -> FirebaseVisionImageMetadata.ROTATION_90
                180 -> FirebaseVisionImageMetadata.ROTATION_180
                270 -> FirebaseVisionImageMetadata.ROTATION_270
                else -> {
                    Timber.e("Bad rotation value: $rotationDegrees")
                    FirebaseVisionImageMetadata.ROTATION_0
                }
            }
        }
        
        try {
            // Get the device's current rotation relative to its "native" orientation
            val deviceRotation = activity.windowManager.defaultDisplay.rotation
            var rotationCompensation = ORIENTATIONS.get(deviceRotation)
            
            // Get camera sensor orientation
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val sensorOrientation = cameraManager
                .getCameraCharacteristics(currentCameraId!!)
                .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                
            // Calculate proper rotation compensation
            rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360
            
            Timber.d("Rotation compensation: deviceRotation=$deviceRotation, " +
                     "sensorOrientation=$sensorOrientation, result=$rotationCompensation")
                     
            // Convert to FirebaseVisionImageMetadata rotation values
            return when (rotationCompensation) {
                0 -> FirebaseVisionImageMetadata.ROTATION_0
                90 -> FirebaseVisionImageMetadata.ROTATION_90
                180 -> FirebaseVisionImageMetadata.ROTATION_180
                270 -> FirebaseVisionImageMetadata.ROTATION_270
                else -> {
                    Timber.e("Bad rotation value after compensation: $rotationCompensation")
                    FirebaseVisionImageMetadata.ROTATION_0
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting rotation compensation")
            // Fallback to simpler method in case of error
            return when (rotationDegrees) {
                0 -> FirebaseVisionImageMetadata.ROTATION_0
                90 -> FirebaseVisionImageMetadata.ROTATION_90
                180 -> FirebaseVisionImageMetadata.ROTATION_180
                270 -> FirebaseVisionImageMetadata.ROTATION_270
                else -> FirebaseVisionImageMetadata.ROTATION_0
            }
        }
    }
    
    fun setFaceDetectionCallback(callback: (List<DetectedFace>) -> Unit) {
        faceDetectionCallback = callback
    }
    
    fun setPreviewDimensionsCallback(callback: (Int, Int) -> Unit) {
        previewDimensionsCallback = callback
    }
    
    fun getCameraPreviewDimensions(): Pair<Int, Int> {
        return Pair(previewWidth, previewHeight)
    }
    
    fun clearFaceDetectionCallback() {
        faceDetectionCallback = null
    }
} 