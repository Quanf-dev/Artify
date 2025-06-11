package com.example.camera.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraSettings(
    val isBackCamera: Boolean = true,
    val flashMode: FlashMode = FlashMode.AUTO,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_4_3,
    val timerSeconds: Int = 0,
    val zoomLevel: Float = 1.0f,
    val brightnessLevel: Float = 0.5f,
    val currentFilter: FilterType = FilterType.NONE,
    val selectedFilter: FilterType = FilterType.NONE
) : Parcelable

enum class FlashMode {
    OFF, ON, AUTO
}

enum class AspectRatio(val ratio: String, val value: Float) {
    RATIO_1_1("1:1", 1.0f),
    RATIO_4_3("4:3", 4.0f / 3.0f),
    RATIO_16_9("16:9", 16.0f / 9.0f),
    RATIO_3_4("3:4", 3.0f / 4.0f),
    RATIO_FULL("Full", 0.0f)
}

enum class FilterType {
    NONE,
    SEPIA,
    BLACK_WHITE,
    CINEMATIC,
    VINTAGE,
    COLD,
    WARM
} 