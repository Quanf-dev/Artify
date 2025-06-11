package com.example.camera.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.example.camera.domain.model.AspectRatio
import com.example.camera.domain.model.FilterType
import com.example.camera.ui.CameraActivity

/**
 * Utility class để launch Camera module một cách dễ dàng
 */
object CameraLauncher {

    /**
     * Mở Camera từ Activity bất kỳ
     */
    fun openCamera(context: Context) {
        CameraActivity.openCamera(context)
    }

    /**
     * Mở Camera với Face Detection
     */
    fun openCameraWithFaceDetection(context: Context) {
        CameraActivity.openCameraWithFaceDetection(context)
    }

    /**
     * Mở Camera với chụp ảnh tự động
     */
    fun openCameraWithAutoCapture(context: Context) {
        CameraActivity.openCameraWithAutoCapture(context)
    }

    /**
     * Tạo Intent tùy chỉnh cho Camera
     */
    fun createCustomCameraIntent(
        context: Context,
        enableFaceDetection: Boolean = true,
        defaultFilter: FilterType = FilterType.NONE,
        aspectRatio: AspectRatio = AspectRatio.RATIO_4_3,
        autoCapture: Boolean = false
    ): Intent {
        return Intent(context, CameraActivity::class.java).apply {
            action = if (autoCapture) {
                CameraActivity.ACTION_CAPTURE_PHOTO
            } else {
                CameraActivity.ACTION_OPEN_CAMERA
            }
            putExtra(CameraActivity.EXTRA_AUTO_CAPTURE, autoCapture)
            putExtra(CameraActivity.EXTRA_ENABLE_FACE_DETECTION, enableFaceDetection)
            putExtra(CameraActivity.EXTRA_DEFAULT_FILTER, defaultFilter.name)
            putExtra(CameraActivity.EXTRA_ASPECT_RATIO, aspectRatio.name)
        }
    }

    /**
     * Builder pattern để tạo Camera Intent dễ dàng
     */
    class Builder(private val context: Context) {
        private var enableFaceDetection = true
        private var defaultFilter = FilterType.NONE
        private var aspectRatio = AspectRatio.RATIO_4_3
        private var autoCapture = false

        fun enableFaceDetection(enable: Boolean) = apply {
            this.enableFaceDetection = enable
        }

        fun setDefaultFilter(filter: FilterType) = apply {
            this.defaultFilter = filter
        }

        fun setAspectRatio(ratio: AspectRatio) = apply {
            this.aspectRatio = ratio
        }

        fun autoCapture(enable: Boolean) = apply {
            this.autoCapture = enable
        }

        fun launch() {
            val intent = createCustomCameraIntent(
                context,
                enableFaceDetection,
                defaultFilter,
                aspectRatio,
                autoCapture
            )
            context.startActivity(intent)
        }

        fun build(): Intent {
            return createCustomCameraIntent(
                context,
                enableFaceDetection,
                defaultFilter,
                aspectRatio,
                autoCapture
            )
        }
    }

    /**
     * Tạo Builder để cấu hình Camera
     */
    fun with(context: Context): Builder {
        return Builder(context)
    }
}

/**
 * Extension functions để gọi Camera dễ dàng hơn
 */

// Cho Activity
fun Activity.openCamera() {
    CameraLauncher.openCamera(this)
}

fun Activity.openCameraWithFaceDetection() {
    CameraLauncher.openCameraWithFaceDetection(this)
}

fun Activity.openCameraWithAutoCapture() {
    CameraLauncher.openCameraWithAutoCapture(this)
}

// Cho Fragment  
fun Fragment.openCamera() {
    requireContext().let { CameraLauncher.openCamera(it) }
}

fun Fragment.openCameraWithFaceDetection() {
    requireContext().let { CameraLauncher.openCameraWithFaceDetection(it) }
}

fun Fragment.openCameraWithAutoCapture() {
    requireContext().let { CameraLauncher.openCameraWithAutoCapture(it) }
}

// Builder pattern extensions
fun Activity.launchCamera(block: CameraLauncher.Builder.() -> Unit) {
    CameraLauncher.with(this).apply(block).launch()
}

fun Fragment.launchCamera(block: CameraLauncher.Builder.() -> Unit) {
    CameraLauncher.with(requireContext()).apply(block).launch()
} 