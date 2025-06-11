package com.example.camera.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.camera.databinding.ActivityCameraBinding
import com.example.camera.ui.camera.CameraFragment
import com.example.camera.ui.camera.CameraViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCameraFragment()
        observeViewModel()
        handleIntent(intent)
    }

    private fun setupCameraFragment() {
        if (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) == null) {
            val cameraFragment = CameraFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, cameraFragment)
                .commit()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Handle camera events if needed
                if (state.capturedPhoto != null && state.showPreview) {
                    // Photo was captured successfully
                    Timber.d("Photo captured: ${state.capturedPhoto.fileName}")
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.action) {
                ACTION_CAPTURE_PHOTO -> {
                    // Direct photo capture mode
                    val autoCapture = it.getBooleanExtra(EXTRA_AUTO_CAPTURE, false)
                    if (autoCapture) {
                        // Auto capture after delay
                        binding.root.postDelayed({
                            // Trigger photo capture
                        }, 1000)
                    }
                }
                ACTION_OPEN_CAMERA -> {
                    // Normal camera mode
                    val enableFaceDetection = it.getBooleanExtra(EXTRA_ENABLE_FACE_DETECTION, true)
                    // Configure camera based on intent extras
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    companion object {
        const val ACTION_CAPTURE_PHOTO = "com.example.camera.CAPTURE_PHOTO"
        const val ACTION_OPEN_CAMERA = "com.example.camera.OPEN_CAMERA"
        
        const val EXTRA_AUTO_CAPTURE = "auto_capture"
        const val EXTRA_ENABLE_FACE_DETECTION = "enable_face_detection"
        const val EXTRA_DEFAULT_FILTER = "default_filter"
        const val EXTRA_ASPECT_RATIO = "aspect_ratio"

        /**
         * Tạo Intent để mở Camera với các tùy chọn
         */
        fun createIntent(
            context: Context,
            autoCapture: Boolean = false,
            enableFaceDetection: Boolean = true,
            defaultFilter: String? = null
        ): Intent {
            return Intent(context, CameraActivity::class.java).apply {
                action = if (autoCapture) ACTION_CAPTURE_PHOTO else ACTION_OPEN_CAMERA
                putExtra(EXTRA_AUTO_CAPTURE, autoCapture)
                putExtra(EXTRA_ENABLE_FACE_DETECTION, enableFaceDetection)
                defaultFilter?.let { putExtra(EXTRA_DEFAULT_FILTER, it) }
            }
        }

        /**
         * Mở Camera ngay lập tức
         */
        fun openCamera(context: Context) {
            val intent = createIntent(context)
            context.startActivity(intent)
        }

        /**
         * Mở Camera với chụp ảnh tự động
         */
        fun openCameraWithAutoCapture(context: Context) {
            val intent = createIntent(context, autoCapture = true)
            context.startActivity(intent)
        }

        /**
         * Mở Camera với Face Detection
         */
        fun openCameraWithFaceDetection(context: Context) {
            val intent = createIntent(context, enableFaceDetection = true)
            context.startActivity(intent)
        }
    }
} 