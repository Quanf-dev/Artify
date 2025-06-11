package com.example.camera.domain.model

import android.graphics.PointF
import android.graphics.Rect
import androidx.annotation.DrawableRes

data class DetectedFace(
    val id: Int,
    val boundingBox: Rect,
    val rotationY: Float,
    val rotationZ: Float,
    val leftEyePosition: PointF?,
    val rightEyePosition: PointF?,
    val nosePosition: PointF?,
    val mouthPosition: PointF?
)

data class FaceMask(
    val id: String,
    val name: String,
    val type: MaskType,
    @DrawableRes val previewImage: Int,
    val overlayResources: List<MaskOverlay>
)

data class MaskOverlay(
    @DrawableRes val drawableRes: Int,
    val anchorPoint: AnchorPoint,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f
)

enum class MaskType {
    FULL_FACE,
    EYES,
    NOSE,
    MOUTH,
    EARS,
    HEAD_ACCESSORY
}

enum class AnchorPoint {
    LEFT_EYE,
    RIGHT_EYE,
    NOSE,
    MOUTH,
    FACE_CENTER,
    LEFT_EAR,
    RIGHT_EAR
} 