package com.example.camera.data.repository

import android.content.Context
import android.graphics.*
import com.example.camera.R
import com.example.camera.domain.model.*
import com.example.camera.domain.repository.FaceDetectionRepository
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceDetectionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FaceDetectionRepository {

    private val faceDetectorOptions = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(faceDetectorOptions)
    private var isDetectionActive = false

    override fun detectFaces(bitmap: Bitmap): Flow<List<DetectedFace>> = flow {
        try {
            Timber.d("Face detection started on bitmap: ${bitmap.width}x${bitmap.height}")
            
            val image = FirebaseVisionImage.fromBitmap(bitmap)
            Timber.d("FirebaseVisionImage created from bitmap")
            
            // Process the image
            val faces = faceDetector.detectInImage(image).await()
            
            if (faces.isEmpty()) {
                Timber.d("No faces detected in the bitmap")
                emit(emptyList())
                return@flow
            }
            
            Timber.d("Detected ${faces.size} faces in bitmap")
            faces.firstOrNull()?.let { firstFace ->
                val hasLeftEye = firstFace.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE) != null
                val hasRightEye = firstFace.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE) != null
                val hasNose = firstFace.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE) != null
                val hasMouth = firstFace.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null
                Timber.d("First face bounds: ${firstFace.boundingBox}, landmarks - left eye: $hasLeftEye, right eye: $hasRightEye, nose: $hasNose, mouth: $hasMouth")
            }
            
            val detectedFaces = faces.mapIndexed { index: Int, face: FirebaseVisionFace ->
                convertToDetectedFace(face, index)
            }
            
            Timber.d("Emitting ${detectedFaces.size} processed faces")
            emit(detectedFaces)
        } catch (e: Exception) {
            Timber.e(e, "Face detection failed: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getAvailableMasks(): List<FaceMask> {
        return listOf(
            FaceMask(
                id = "dog_ears",
                name = "Dog Ears",
                type = MaskType.EARS,
                previewImage = R.drawable.ic_dog_ears_preview,
                overlayResources = listOf(
                    MaskOverlay(
                        drawableRes = R.drawable.dog_ears_left,
                        anchorPoint = AnchorPoint.LEFT_EAR,
                        scaleX = 1.2f,
                        scaleY = 1.2f
                    ),
                    MaskOverlay(
                        drawableRes = R.drawable.dog_ears_right,
                        anchorPoint = AnchorPoint.RIGHT_EAR,
                        scaleX = 1.2f,
                        scaleY = 1.2f
                    )
                )
            ),
            FaceMask(
                id = "sunglasses",
                name = "Cool Sunglasses",
                type = MaskType.EYES,
                previewImage = R.drawable.ic_sunglasses_preview,
                overlayResources = listOf(
                    MaskOverlay(
                        drawableRes = R.drawable.sunglasses,
                        anchorPoint = AnchorPoint.FACE_CENTER,
                        offsetY = -20.0f
                    )
                )
            ),
            FaceMask(
                id = "cat_face",
                name = "Cat Face",
                type = MaskType.FULL_FACE,
                previewImage = R.drawable.ic_cat_face_preview,
                overlayResources = listOf(
                    MaskOverlay(
                        drawableRes = R.drawable.cat_whiskers,
                        anchorPoint = AnchorPoint.NOSE,
                        scaleX = 1.5f,
                        scaleY = 1.5f
                    ),
                    MaskOverlay(
                        drawableRes = R.drawable.cat_nose,
                        anchorPoint = AnchorPoint.NOSE
                    )
                )
            ),
            FaceMask(
                id = "mustache",
                name = "Mustache",
                type = MaskType.MOUTH,
                previewImage = R.drawable.ic_mustache_preview,
                overlayResources = listOf(
                    MaskOverlay(
                        drawableRes = R.drawable.mustache,
                        anchorPoint = AnchorPoint.MOUTH,
                        offsetY = -10.0f
                    )
                )
            )
        )
    }

    override suspend fun applyMaskToFace(
        originalBitmap: Bitmap,
        detectedFaces: List<DetectedFace>,
        selectedMask: FaceMask
    ): Bitmap {
        val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        detectedFaces.forEach { face ->
            selectedMask.overlayResources.forEach { overlay ->
                drawMaskOverlay(canvas, face, overlay)
            }
        }

        return resultBitmap
    }

    private var faceDetectionChannel = kotlinx.coroutines.channels.Channel<List<DetectedFace>>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    override fun startRealTimeFaceDetection(): Flow<List<DetectedFace>> = flow {
        isDetectionActive = true
        Timber.d("Starting real-time face detection with camera frames")
        
        try {
            while (isDetectionActive) {
                val faces = faceDetectionChannel.tryReceive().getOrNull()
                if (faces != null) {
                    Timber.d("Emitting ${faces.size} detected faces")
                    emit(faces)
                }
                kotlinx.coroutines.delay(50) // High frequency updates for smooth tracking
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in real-time face detection")
            emit(emptyList())
        }
    }
    
    fun updateDetectedFaces(faces: List<DetectedFace>) {
        if (isDetectionActive) {
            faceDetectionChannel.trySend(faces)
        }
    }

    override fun stopRealTimeFaceDetection() {
        isDetectionActive = false
    }

    override fun isDetectionActive(): Boolean = isDetectionActive

    private fun convertToDetectedFace(face: FirebaseVisionFace, id: Int): DetectedFace {
        return DetectedFace(
            id = face.trackingId.takeIf { it != FirebaseVisionFace.INVALID_ID } ?: id,
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

    private fun drawMaskOverlay(canvas: Canvas, face: DetectedFace, overlay: MaskOverlay) {
        val anchorPosition = getAnchorPosition(face, overlay.anchorPoint) ?: return
        
        try {
            val drawable = context.getDrawable(overlay.drawableRes) ?: return
            val bitmap = drawableToBitmap(drawable)
            
            val scaledWidth = (bitmap.width * overlay.scaleX).toInt()
            val scaledHeight = (bitmap.height * overlay.scaleY).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            
            val x = anchorPosition.x - scaledWidth / 2 + overlay.offsetX
            val y = anchorPosition.y - scaledHeight / 2 + overlay.offsetY
            
            canvas.drawBitmap(scaledBitmap, x, y, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to draw mask overlay")
        }
    }

    private fun getAnchorPosition(face: DetectedFace, anchorPoint: AnchorPoint): PointF? {
        return when (anchorPoint) {
            AnchorPoint.LEFT_EYE -> face.leftEyePosition
            AnchorPoint.RIGHT_EYE -> face.rightEyePosition
            AnchorPoint.NOSE -> face.nosePosition
            AnchorPoint.MOUTH -> face.mouthPosition
            AnchorPoint.FACE_CENTER -> PointF(
                face.boundingBox.centerX().toFloat(),
                face.boundingBox.centerY().toFloat()
            )
            AnchorPoint.LEFT_EAR -> PointF(
                face.boundingBox.left.toFloat() - 20f,
                face.boundingBox.top.toFloat() + face.boundingBox.height() * 0.3f
            )
            AnchorPoint.RIGHT_EAR -> PointF(
                face.boundingBox.right.toFloat() + 20f,
                face.boundingBox.top.toFloat() + face.boundingBox.height() * 0.3f
            )
        }
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
} 