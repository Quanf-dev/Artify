package com.example.camera.filter

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.Camera
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import com.iammert.library.cameravideobuttonlib.CameraVideoButton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import kotlin.math.abs

class FaceFilterActivity : AppCompatActivity() {

    // Aspect ratio definitions
    private enum class AspectRatio(val ratio: Float) {
        RATIO_1_1(1.0f),           // 1.0
        FULL(-1f)                  // -1f: full screen, sẽ xử lý riêng
    }

    // Camera and detection properties
    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay? = null

    // Filter and camera state
    private var currentFilterType: Int = 0
    private var isFlashEnabled: Boolean = false
    private var isZoomVisible: Boolean = false
    private var currentTimerSeconds: Int = 0
    private var cameraFacing: Int = CameraSource.CAMERA_FACING_FRONT
    private var currentAspectRatio: AspectRatio = AspectRatio.FULL
    private var brightness: Float = 0.0f // -1.0 to 1.0

    // Zoom functionality
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var scaleFactor: Float = 1.0f
    private val maxZoom: Float = 5.0f
    private val minZoom: Float = 1.0f

    // Timer functionality
    private var countDownTimer: CountDownTimer? = null
    private var timerTextView: TextView? = null

    // UI elements
    private var faceButton: ImageButton? = null
    private var flashButton: ImageButton? = null
    private var cameraButton: CameraVideoButton? = null
    private var timerButton: ImageButton? = null
    private var switchCameraButton: ImageButton? = null
    private var zoomSeekBar: SeekBar? = null
    private var brightnessSeekBar: SeekBar? = null
    private var aspectRatioButton: ImageButton? = null
    private var zoomToggleButton: ImageButton? = null
    
    // List to store captured image paths
    private val capturedImagePaths: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_filter)

        initializeViews()
        setupZoomGesture()
        checkCameraPermission()
    }

    private fun initializeViews() {
        mPreview = findViewById(R.id.preview)
        mGraphicOverlay = findViewById(R.id.faceOverlay)
        timerTextView = findViewById(R.id.timerText)

        // Initialize buttons
        faceButton = findViewById(R.id.face)
        flashButton = findViewById(R.id.flash)
        cameraButton = findViewById(R.id.camera)
        timerButton = findViewById(R.id.timer)
        switchCameraButton = findViewById(R.id.change)
        zoomSeekBar = findViewById(R.id.seekBarZoom)
        brightnessSeekBar = findViewById(R.id.seekBarBrightness)
        aspectRatioButton = findViewById(R.id.aspect_ratio_button)
        zoomToggleButton = findViewById(R.id.zoom_toggle_button)

        // Hide zoom and brightness seekbars by default
        zoomSeekBar?.visibility = View.GONE
        brightnessSeekBar?.visibility = View.GONE

        setupUIClickListeners()
        setupFilterButtons()
    }

    private fun setupZoomGesture() {
        scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(minZoom, maxZoom)
                    setZoom(scaleFactor)

                    // Update seekbar progress
                    val progress = ((scaleFactor - minZoom) / (maxZoom - minZoom) * 100).toInt()
                    zoomSeekBar?.progress = progress
                    return true
                }
            })

        // Set touch listener to preview
        mPreview?.setOnTouchListener { _, event ->
            scaleGestureDetector?.onTouchEvent(event)
            true
        }
    }

    private fun setupUIClickListeners() {
        faceButton?.setOnClickListener { toggleFilterVisibility() }
        flashButton?.setOnClickListener { toggleFlash() }
        
        // Cấu hình CameraVideoButton
        cameraButton?.apply {
            enablePhotoTaking(true)
            enableVideoRecording(false) // Tạm thời tắt chức năng quay video
            
            actionListener = object : com.iammert.library.cameravideobuttonlib.CameraVideoButton.ActionListener {
                override fun onStartRecord() {
                    Log.d(TAG, "onStartRecord called")
                    // Không xử lý quay video trong phiên bản này
                }

                override fun onEndRecord() {
                    Log.d(TAG, "onEndRecord called")
                    // Không xử lý quay video trong phiên bản này
                }

                override fun onDurationTooShortError() {
                    Log.d(TAG, "onDurationTooShortError called")
                }

                override fun onSingleTap() {
                    Log.d(TAG, "onSingleTap called - taking photo")
                    capturePhoto()
                }
            }
        }
        
        timerButton?.setOnClickListener { showTimerDialog() }
        switchCameraButton?.setOnClickListener { switchCamera() }
        aspectRatioButton?.setOnClickListener { showAspectRatioDialog() }
        zoomToggleButton?.setOnClickListener { toggleZoomVisibility() }

        zoomSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val scale = minZoom + (progress / 100f) * (maxZoom - minZoom)
                    setZoom(scale)
                    scaleFactor = scale // Update scaleFactor for consistency
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        brightnessSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    brightness = (progress - 50) / 50f // Convert 0-100 to -1.0 to 1.0
                    setBrightness(brightness)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupFilterButtons() {
        val filterButtons = mapOf(
            R.id.no_filter to 0, R.id.hair to 1, R.id.op to 2, R.id.snap to 3,
            R.id.glasses2 to 4, R.id.glasses3 to 5, R.id.glasses4 to 6, R.id.glasses5 to 7,
            R.id.mask2 to 8, R.id.mask3 to 9, R.id.dog to 10, R.id.cat2 to 11,
            R.id.hat to 12, R.id.hat2 to 13, R.id.spiderman to 14, R.id.songoku to 15,
            R.id.ronaldo to 16
        )

        filterButtons.forEach { (buttonId, filterType) ->
            findViewById<ImageButton>(buttonId)?.setOnClickListener {
                selectFilter(filterType)
            }
        }
    }

    private fun toggleFilterVisibility() {
        val scrollView = findViewById<View>(R.id.scrollView)
        val isVisible = scrollView.visibility == View.VISIBLE

        scrollView.visibility = if (isVisible) View.GONE else View.VISIBLE

        val iconResource = if (isVisible) R.drawable.face else R.drawable.face_select
        faceButton?.setImageResource(iconResource)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun selectFilter(filterType: Int) {
        findViewById<View>(FILTER_BUTTON_IDS[currentFilterType])?.background =
            ContextCompat.getDrawable(this, R.drawable.round_background)

        currentFilterType = filterType

        findViewById<View>(FILTER_BUTTON_IDS[currentFilterType])?.background =
            ContextCompat.getDrawable(this, R.drawable.round_background_select)

        refreshFaceDetection()
    }

    private fun toggleFlash() {
        isFlashEnabled = !isFlashEnabled

        val tintColor = if (isFlashEnabled) Color.YELLOW else Color.WHITE
        flashButton?.setColorFilter(tintColor)

        setFlashMode(if (isFlashEnabled) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF)
    }

    private fun setFlashMode(flashMode: String) {
        mCameraSource?.let { source ->
            try {
                val camera = getCamera(source)
                camera?.let {
                    val params = it.parameters
                    if (params.supportedFlashModes?.contains(flashMode) == true) {
                        params.flashMode = flashMode
                        it.parameters = params
                        Log.d(TAG, "Flash mode set to $flashMode")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set flash mode", e)
            }
        }
    }

    private fun getCamera(cameraSource: CameraSource): Camera? {
        val fields = CameraSource::class.java.declaredFields
        for (field in fields) {
            if (field.type == Camera::class.java) {
                field.isAccessible = true
                try {
                    return field.get(cameraSource) as Camera?
                } catch (e: IllegalAccessException) {
                    Log.e(TAG, "Failed to get camera instance", e)
                }
            }
        }
        return null
    }

    private fun showTimerDialog() {
        val timerOptions = arrayOf("Off", "3s", "5s", "10s")
        val timerValues = intArrayOf(0, 3, 5, 10)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.timer_title))
            .setItems(timerOptions) { _, which ->
                currentTimerSeconds = timerValues[which]
                updateTimerButton()
            }
            .show()
    }

    private fun updateTimerButton() {
        val tintColor = if (currentTimerSeconds > 0) Color.YELLOW else Color.WHITE
        timerButton?.setColorFilter(tintColor)
    }

    private fun switchCamera() {
        Log.d(TAG, "Switching camera")
        mPreview?.stop()
        mCameraSource?.release()
        mCameraSource = null

        cameraFacing = if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) {
            CameraSource.CAMERA_FACING_BACK
        } else {
            CameraSource.CAMERA_FACING_FRONT
        }

        createCameraSource()
        startCameraSource()
    }

    private fun capturePhoto() {
        if (currentTimerSeconds > 0) {
            startTimer()
        } else {
            takePhoto()
        }
    }

    private fun startTimer() {
        timerTextView?.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer((currentTimerSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView?.text = (millisUntilFinished / 1000 + 1).toString()
            }

            override fun onFinish() {
                timerTextView?.visibility = View.GONE
                takePhoto()
            }
        }.start()
    }

    private fun takePhoto() {
        try {
            if (mCameraSource == null) {
                Log.e(TAG, "CameraSource is null, cannot take picture")
                runOnUiThread { showErrorMessage("Camera is not ready") }
                return
            }
            val camera = getCamera(mCameraSource!!)
            if (camera == null) {
                Log.e(TAG, "Camera is not available for takePicture")
                runOnUiThread { showErrorMessage("Camera is not available") }
                return
            }
            
            // Chụp ảnh từ camera
            mCameraSource?.takePicture(null, object : CameraSource.PictureCallback {
                override fun onPictureTaken(bytes: ByteArray?) {
                    if (bytes == null || bytes.isEmpty()) {
                        Log.e(TAG, "onPictureTaken: received null or empty bytes")
                        runOnUiThread { showErrorMessage("Failed to capture image data (empty)") }
                        return
                    }
                    try {
                        Log.d(TAG, "Photo captured, processing image...")
                        
                        // Xử lý ảnh từ camera
                        val cameraBitmap = try {
                            processCapturedImage(bytes)
                        } catch (e: Exception) {
                            Log.e(TAG, "Bitmap decode failed", e)
                            runOnUiThread { showErrorMessage("Failed to decode image: ${e.message}") }
                            return
                        }
                        
                        Log.d(TAG, "Camera bitmap processed: ${cameraBitmap.width}x${cameraBitmap.height}")
                        
                        // Tạo bitmap kết hợp với overlay
                        val finalBitmap = createFinalImageWithOverlay(cameraBitmap)
                        
                        Log.d(TAG, "Final bitmap created: ${finalBitmap.width}x${finalBitmap.height}")
                        
                        // Lưu và hiển thị ảnh
                        saveBitmapToCacheAndPreview(finalBitmap)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing captured image", e)
                        runOnUiThread { showErrorMessage("Failed to save photo: ${e.message}") }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "takePicture failed", e)
            runOnUiThread { showErrorMessage("Camera error: ${e.message}") }
        }
    }
    
    /**
     * Tạo bitmap cuối cùng kết hợp ảnh từ camera với các filter và mask từ GraphicOverlay
     */
    private fun createFinalImageWithOverlay(cameraBitmap: Bitmap): Bitmap {
        Log.d(TAG, "Creating final image with overlay")
        
        try {
            // Tạo bitmap mới với kích thước của ảnh camera
            val resultBitmap = Bitmap.createBitmap(
                cameraBitmap.width, 
                cameraBitmap.height, 
                Bitmap.Config.ARGB_8888
            )
            
            // Tạo canvas để vẽ
            val canvas = Canvas(resultBitmap)
            
            // Vẽ ảnh camera làm nền
            canvas.drawBitmap(cameraBitmap, 0f, 0f, null)
            
            // Vẽ face filter overlay nếu có
            drawFaceFiltersOnBitmap(canvas, cameraBitmap.width, cameraBitmap.height)
            
            Log.d(TAG, "Final image created successfully: ${resultBitmap.width}x${resultBitmap.height}")
            return resultBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error creating final image: ${e.message}", e)
            // Nếu có lỗi, trả về ảnh gốc từ camera
            return cameraBitmap
        }
    }

    // Hàm mới để vẽ face filters lên bitmap
    private fun drawFaceFiltersOnBitmap(canvas: Canvas, imageWidth: Int, imageHeight: Int) {
        // Chỉ vẽ face filter nếu currentFilterType > 0 (không phải no_filter)
        if (currentFilterType <= 0) return
        
        try {
            // Lấy thông tin face từ overlay hiện tại
            mGraphicOverlay?.let { overlay ->
                val graphics = overlay.getGraphics()
                
                // Tính toán scale factors để map từ overlay coordinates sang image coordinates
                val scaleX = imageWidth.toFloat() / overlay.width.toFloat()
                val scaleY = imageHeight.toFloat() / overlay.height.toFloat()
                
                // Vẽ từng graphic
                for (graphic in graphics) {
                    if (graphic is FaceGraphic) {
                        // Tạo canvas tạm thời với scale factor
                        canvas.save()
                        canvas.scale(scaleX, scaleY)
                        graphic.draw(canvas)
                        canvas.restore()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing face filters on bitmap: ${e.message}", e)
        }
    }

    private fun saveBitmapToCacheAndPreview(bitmap: Bitmap) {
        val imageFile = try {
            saveImageToCache(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image to cache", e)
            runOnUiThread { showErrorMessage("Failed to save image: ${e.message}") }
            return
        }

        runOnUiThread {
            if (!imageFile.exists() || imageFile.length() == 0L) {
                Log.e(TAG, "Image file does not exist or is empty: ${imageFile.absolutePath}")
                showErrorMessage("Image file not saved correctly")
                return@runOnUiThread
            }
            
            // Add the image path to our list
            capturedImagePaths.add(imageFile.absolutePath)
            
            try {
                val intent = Intent(
                    this@FaceFilterActivity,
                    com.example.camera.ui.preview.PreviewActivity::class.java
                ).apply {
                    putExtra(
                        com.example.camera.ui.preview.PreviewActivity.EXTRA_IMAGE_PATH,
                        imageFile.absolutePath
                    )
                    // Pass the array of all captured image paths
                    putStringArrayListExtra(
                        com.example.camera.ui.preview.PreviewActivity.EXTRA_IMAGE_PATHS,
                        capturedImagePaths
                    )
                    // Pass the current position in the array
                    putExtra(
                        com.example.camera.ui.preview.PreviewActivity.EXTRA_CURRENT_POSITION,
                        capturedImagePaths.size - 1
                    )
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to navigate to preview", e)
                showErrorMessage("Failed to open preview: ${e.message}")
            }
        }
    }

    private fun processCapturedImage(bytes: ByteArray): Bitmap {
        val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("Failed to decode byte array.")

        // Tính toán góc xoay dựa trên hướng camera
        val rotation = if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) 270f else 90f
        
        // Tạo matrix cho việc xoay và lật ảnh
        val matrix = Matrix().apply {
            postRotate(rotation)
            if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) {
                // Lật ảnh ngang cho camera trước
                postScale(-1f, 1f, decodedBitmap.width / 2f, decodedBitmap.height / 2f)
            }
        }

        // Xoay ảnh theo góc đã tính
        val rotatedBitmap = Bitmap.createBitmap(
            decodedBitmap,
            0,
            0,
            decodedBitmap.width,
            decodedBitmap.height,
            matrix,
            false
        )

        // Cắt ảnh theo tỉ lệ đã chọn để khớp với preview
        val finalBitmap = when (currentAspectRatio) {
            AspectRatio.RATIO_1_1 -> cropToAspectRatioFromCenter(rotatedBitmap, 1f)
            AspectRatio.FULL -> {
                // Cho FULL, cắt theo tỉ lệ của preview để đảm bảo nhất quán
                val previewRatio = getPreviewAspectRatio()
                cropToAspectRatioFromCenter(rotatedBitmap, previewRatio)
            }
        }

        // Giới hạn kích thước ảnh nếu cần nhưng giữ nguyên tỉ lệ
        return resizeBitmapKeepRatio(finalBitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
    }

    // Hàm mới để cắt ảnh từ center theo tỉ lệ chính xác
    private fun cropToAspectRatioFromCenter(bitmap: Bitmap, targetRatio: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val currentRatio = width.toFloat() / height.toFloat()

        return if (abs(currentRatio - targetRatio) < 0.01f) {
            // Tỉ lệ hiện tại đã gần đúng, không cần cắt
            bitmap
        } else if (currentRatio > targetRatio) {
            // Ảnh quá rộng, cần cắt chiều rộng từ center
            val newWidth = (height * targetRatio).toInt()
            val x = (width - newWidth) / 2
            Bitmap.createBitmap(bitmap, x, 0, newWidth, height)
        } else {
            // Ảnh quá cao, cần cắt chiều cao từ center
            val newHeight = (width / targetRatio).toInt()
            val y = (height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, y, width, newHeight)
        }
    }

    // Hàm mới để resize bitmap nhưng giữ nguyên tỉ lệ
    private fun resizeBitmapKeepRatio(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return bitmap
        }

        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()

        var finalWidth = maxWidth
        var finalHeight = (finalWidth / ratio).toInt()

        if (finalHeight > maxHeight) {
            finalHeight = maxHeight
            finalWidth = (finalHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    // Hàm mới để lấy aspect ratio của preview
    private fun getPreviewAspectRatio(): Float {
        return mPreview?.let { preview ->
            val previewWidth = preview.width
            val previewHeight = preview.height
            if (previewHeight > 0) {
                previewWidth.toFloat() / previewHeight.toFloat()
            } else {
                getScreenAspectRatio()
            }
        } ?: getScreenAspectRatio()
    }

    private fun saveImageToCache(bitmap: Bitmap): File {
        val cachePath = File(cacheDir, "images")
        if (!cachePath.exists()) {
            val created = cachePath.mkdirs()
            Log.d(TAG, "Cache dir created: $created at ${cachePath.absolutePath}")
        }
        
        // Tạo tên file với timestamp để tránh trùng lặp
        val timestamp = System.currentTimeMillis()
        val filename = "captured_image_$timestamp.jpg"
        val file = File(cachePath, filename)
        
        try {
            FileOutputStream(file).use { out ->
                // Sử dụng chất lượng cao hơn (95) để đảm bảo ảnh rõ nét
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                Log.d(TAG, "Bitmap compress success: $success, file: ${file.absolutePath}, size: ${file.length()} bytes")
                
                if (!success) {
                    throw IOException("Bitmap compress failed")
                }
                
                // Đảm bảo dữ liệu được ghi xuống đĩa
                out.flush()
            }
            
            if (!file.exists() || file.length() == 0L) {
                throw IOException("File not written: ${file.absolutePath}")
            }
            
            Log.d(TAG, "Image saved successfully: ${file.absolutePath}, size: ${file.length()} bytes, dimensions: ${bitmap.width}x${bitmap.height}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to cache: ${e.message}", e)
            throw e
        }
    }

    private fun getImageStorageFolder(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "FaceFilter"
        )
    }

    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Camera permission granted, creating camera source")
            createCameraSource()
        } else {
            Log.d(TAG, "Camera permission not granted, requesting permission")
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }
        Snackbar.make(
            mGraphicOverlay!!,
            R.string.permission_camera_rationale,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.ok) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    RC_HANDLE_CAMERA_PERM
                )
            }
            .show()
    }

    private fun setZoom(scale: Float) {
        mCameraSource?.let { source ->
            val camera = getCamera(source)
            camera?.let {
                try {
                    val params = it.parameters
                    if (params.isZoomSupported) {
                        val maxCameraZoom = params.maxZoom
                        val zoom = ((scale - minZoom) / (maxZoom - minZoom) * maxCameraZoom).toInt()
                            .coerceIn(0, maxCameraZoom)
                        if (params.zoom != zoom) {
                            params.zoom = zoom
                            it.parameters = params
                            Log.d(TAG, "Zoom set to $zoom (scale $scale)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set zoom", e)
                }
            } ?: Log.w(TAG, "Could not get camera for zoom")
        }
    }

    private fun showAspectRatioDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_aspect_ratio, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.aspectRatio11).setOnClickListener {
            setAspectRatio(AspectRatio.RATIO_1_1)
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.aspectRatioFull).setOnClickListener {
            setAspectRatio(AspectRatio.FULL)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun setAspectRatio(aspectRatio: AspectRatio) {
        if (currentAspectRatio == aspectRatio) return

        currentAspectRatio = aspectRatio
        Log.d(TAG, "Setting aspect ratio to: ${aspectRatio.ratio}")

        // Đặt aspect ratio cho preview
        val previewRatio = when (aspectRatio) {
            AspectRatio.FULL -> -1f // Sử dụng full screen
            else -> aspectRatio.ratio
        }
        
        mPreview?.setAspectRatio(previewRatio)

        // Restart camera với cài đặt mới
        mPreview?.stop()
        mCameraSource?.release()
        mCameraSource = null
        createCameraSource()
        startCameraSource()
    }

    private fun createCameraSource() {
        Log.d(TAG, "Creating camera source for facing: $cameraFacing")

        val detector = FaceDetector.Builder(applicationContext)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .setProminentFaceOnly(true)
            .setTrackingEnabled(true)
            .setMode(FaceDetector.ACCURATE_MODE)
            .setMinFaceSize(MIN_FACE_SIZE)
            .build()

        detector.setProcessor(MultiProcessor.Builder<Face?>(GraphicFaceTrackerFactory()).build())

        if (!detector.isOperational()) {
            handleDetectorNotOperational()
            return
        }

        val targetRatio = if (currentAspectRatio == AspectRatio.FULL) {
            getScreenAspectRatio()
        } else {
            currentAspectRatio.ratio
        }

        val tempCamera = try {
            Camera.open(if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open temporary camera", e)
            showErrorMessage("Could not configure camera.")
            return
        }

        try {
            val params = tempCamera.parameters
            val bestPreviewSize = findBestSize(params.supportedPreviewSizes, targetRatio)
            val bestPictureSize = findBestSize(params.supportedPictureSizes, targetRatio)

            val builder = CameraSource.Builder(applicationContext, detector)
                .setFacing(cameraFacing)
                .setAutoFocusEnabled(true)
                .setRequestedFps(PREVIEW_FPS)
                .setRequestedPreviewSize(bestPreviewSize.width, bestPreviewSize.height)

            mCameraSource = builder.build()

            // Set picture size directly on the camera instance
            getCamera(mCameraSource!!)?.let { camera ->
                val camParams = camera.parameters
                camParams.setPictureSize(bestPictureSize.width, bestPictureSize.height)
                camera.parameters = camParams
                Log.d(TAG, "Set picture size to: ${bestPictureSize.width}x${bestPictureSize.height}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create camera source or set parameters.", e)
            showErrorMessage("Failed to setup camera.")
        } finally {
            tempCamera.release()
        }

        Log.d(TAG, "Camera source created successfully")
    }

    private fun handleDetectorNotOperational() {
        Log.w(TAG, "Face detector dependencies not yet available.")
        if (registerReceiver(null, IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)) != null) {
            showErrorMessage("Face detection requires more storage space")
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (mCameraSource == null) createCameraSource()
            startCameraSource()
        } else {
            requestCameraPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        mPreview?.stop()
        countDownTimer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPreview?.release()
        mCameraSource?.release()
        countDownTimer?.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted - creating camera source")
                createCameraSource()
            } else {
                Log.e(TAG, "Permission not granted")
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Face Filter")
            .setMessage(R.string.no_camera_permission)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }

    private fun startCameraSource() {
        Log.d(TAG, "Starting camera source")
        val code =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)?.show()
            return
        }

        mCameraSource?.let {
            try {
                mPreview?.start(it, mGraphicOverlay)
                Log.d(TAG, "Camera preview started")
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source", e)
                it.release()
                mCameraSource = null
                showErrorMessage("Could not start camera")
            }
        }
    }

    private fun refreshFaceDetection() {
        mGraphicOverlay?.clear()
    }

    private inner class GraphicFaceTrackerFactory : MultiProcessor.Factory<Face?> {
        override fun create(face: Face?): Tracker<Face?> = GraphicFaceTracker(mGraphicOverlay!!)
    }

    private inner class GraphicFaceTracker(private val overlay: GraphicOverlay) : Tracker<Face?>() {
        private val faceGraphic: FaceGraphic = FaceGraphic(overlay, currentFilterType)
        override fun onNewItem(id: Int, item: Face?) = faceGraphic.setId(id)
        override fun onUpdate(detections: Detector.Detections<Face?>, face: Face?) {
            face?.let {
                overlay.add(faceGraphic)
                faceGraphic.updateFace(it, currentFilterType)
            }
        }

        override fun onMissing(detections: Detector.Detections<Face?>) = overlay.remove(faceGraphic)
        override fun onDone() = overlay.remove(faceGraphic)
    }

    private fun getScreenAspectRatio(): Float {
        val displayMetrics = resources.displayMetrics
        // Return a ratio that matches camera sensor orientation (width > height)
        return if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
            displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat()
        } else {
            displayMetrics.heightPixels.toFloat() / displayMetrics.widthPixels.toFloat()
        }
    }

    private fun findBestSize(supportedSizes: List<Camera.Size>, targetRatio: Float): Camera.Size {
        var bestSize = supportedSizes[0]
        var minDiff = Float.MAX_VALUE

        for (size in supportedSizes) {
            val ratio = size.width.toFloat() / size.height.toFloat()
            val diff = abs(ratio - targetRatio)

            if (diff < minDiff) {
                bestSize = size
                minDiff = diff
            } else if (diff == minDiff && size.width > bestSize.width) {
                // If diff is the same, prefer the larger size for better quality
                bestSize = size
            }
        }
        Log.d(TAG, "Best size for ratio $targetRatio is ${bestSize.width}x${bestSize.height}")
        return bestSize
    }

    private fun toggleZoomVisibility() {
        isZoomVisible = !isZoomVisible
        zoomSeekBar?.visibility = if (isZoomVisible) View.VISIBLE else View.GONE
        brightnessSeekBar?.visibility = if (isZoomVisible) View.VISIBLE else View.GONE
        
        val tintColor = if (isZoomVisible) Color.YELLOW else Color.WHITE
        zoomToggleButton?.setColorFilter(tintColor)
    }


    private fun setBrightness(brightness: Float) {
        mCameraSource?.let { source ->
            try {
                val camera = getCamera(source)
                camera?.let {
                    val params = it.parameters
                    // Exposure compensation range is typically -4 to +4
                    val exposureRange = params.maxExposureCompensation - params.minExposureCompensation
                    if (exposureRange > 0) {
                        val exposureValue = (brightness * exposureRange / 2).toInt()
                            .coerceIn(params.minExposureCompensation, params.maxExposureCompensation)
                        params.exposureCompensation = exposureValue
                        it.parameters = params
                        Log.d(TAG, "Brightness set to $brightness (exposure: $exposureValue)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set brightness", e)
            }
        }
    }

    companion object {
        private const val TAG = "FaceFilterActivity"
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480
        private const val PREVIEW_FPS = 30.0f
        private const val MIN_FACE_SIZE = 0.15f
        private const val MAX_IMAGE_WIDTH = 1280
        private const val MAX_IMAGE_HEIGHT = 720
        private const val JPEG_QUALITY = 95
        private const val RC_HANDLE_GMS = 9001
        private const val RC_HANDLE_CAMERA_PERM = 2

        private val FILTER_BUTTON_IDS = intArrayOf(
            R.id.no_filter, R.id.hair, R.id.op, R.id.snap,
            R.id.glasses2, R.id.glasses3, R.id.glasses4, R.id.glasses5,
            R.id.mask2, R.id.mask3, R.id.dog, R.id.cat2,
            R.id.hat, R.id.hat2, R.id.spiderman, R.id.songoku,
            R.id.ronaldo
        )
    }
}

