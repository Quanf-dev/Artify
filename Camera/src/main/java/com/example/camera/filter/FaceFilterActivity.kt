package com.example.camera.filter

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date

class FaceFilterActivity : AppCompatActivity() {

    // Camera and detection properties
    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay? = null

    // Filter and camera state
    private var currentFilterType: Int = 0
    private var isFlashEnabled: Boolean = false
    private var isGridEnabled: Boolean = false
    private var currentTimerSeconds: Int = 0
    private var cameraFacing: Int = CameraSource.CAMERA_FACING_FRONT
    
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
    private var cameraButton: ImageButton? = null
    private var gridButton: ImageButton? = null
    private var timerButton: ImageButton? = null
    private var switchCameraButton: ImageButton? = null
    private var zoomSeekBar: SeekBar? = null

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
        gridButton = findViewById(R.id.grid)
        timerButton = findViewById(R.id.timer)
        switchCameraButton = findViewById(R.id.change)
        zoomSeekBar = findViewById(R.id.seekBarZoom)
        
        setupUIClickListeners()
        setupFilterButtons()
    }

    private fun setupZoomGesture() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
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
        cameraButton?.setOnClickListener { capturePhoto() }
        gridButton?.setOnClickListener { toggleGrid() }
        timerButton?.setOnClickListener { showTimerDialog() }
        switchCameraButton?.setOnClickListener { switchCamera() }

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
    }

    private fun setupFilterButtons() {
        val filterButtons = mapOf(
            R.id.no_filter to 0, R.id.hair to 1, R.id.op to 2, R.id.snap to 3,
            R.id.glasses2 to 4, R.id.glasses3 to 5, R.id.glasses4 to 6, R.id.glasses5 to 7,
            R.id.mask2 to 8, R.id.mask3 to 9, R.id.dog to 10, R.id.cat2 to 11,
            R.id.hat to 12, R.id.hat2 to 13
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

    private fun toggleGrid() {
        isGridEnabled = !isGridEnabled
        val tintColor = if (isGridEnabled) Color.YELLOW else Color.WHITE
        gridButton?.setColorFilter(tintColor)
        mGraphicOverlay?.setGridEnabled(isGridEnabled)
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
            mCameraSource?.takePicture(null, object : CameraSource.PictureCallback {
                override fun onPictureTaken(bytes: ByteArray?) {
                    if (bytes == null || bytes.isEmpty()) {
                        Log.e(TAG, "onPictureTaken: received null or empty bytes")
                        runOnUiThread { showErrorMessage("Failed to capture image data (empty)") }
                        return
                    }
                    try {
                        val bitmap = try {
                            processCapturedImage(bytes)
                        } catch (e: Exception) {
                            Log.e(TAG, "Bitmap decode failed", e)
                            runOnUiThread { showErrorMessage("Failed to decode image: ${e.message}") }
                            return
                        }
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
                            try {
                                val intent = Intent(this@FaceFilterActivity, com.example.camera.ui.preview.PreviewActivity::class.java).apply {
                                    putExtra(com.example.camera.ui.preview.PreviewActivity.EXTRA_IMAGE_PATH, imageFile.absolutePath)
                                }
                                Log.d(TAG, "Navigating to PreviewActivity with path: ${imageFile.absolutePath}")
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to navigate to preview", e)
                                showErrorMessage("Failed to open preview: ${e.message}")
                            }
                        }
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

    private fun processCapturedImage(bytes: ByteArray): Bitmap {
        val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("Failed to decode byte array.")
        
        val rotation = if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) 270f else 90f
        val matrix = Matrix().apply { 
            postRotate(rotation)
            if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) {
                // Mirror the image for front camera
                postScale(-1f, 1f, decodedBitmap.width / 2f, decodedBitmap.height / 2f)
            }
        }
        
        val rotatedBitmap = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, false)
        return resizeBitmap(rotatedBitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (maxWidth <= 0 || maxHeight <= 0) return bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        val finalWidth = if (ratioMax > ratioBitmap) (maxHeight * ratioBitmap).toInt() else maxWidth
        val finalHeight = if (ratioMax > ratioBitmap) maxHeight else (maxWidth / ratioBitmap).toInt()

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun saveImageToCache(bitmap: Bitmap): File {
        val cachePath = File(cacheDir, "images")
        if (!cachePath.exists()) {
            val created = cachePath.mkdirs()
            Log.d(TAG, "Cache dir created: $created at ${cachePath.absolutePath}")
        }
        val file = File(cachePath, "captured_image_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            Log.d(TAG, "Bitmap compress success: $success, file: ${file.absolutePath}")
            if (!success) throw IOException("Bitmap compress failed")
        }
        if (!file.exists() || file.length() == 0L) throw IOException("File not written: ${file.absolutePath}")
        return file
    }

    private fun getImageStorageFolder(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FaceFilter")
    }

    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted, creating camera source")
            createCameraSource()
        } else {
            Log.d(TAG, "Camera permission not granted, requesting permission")
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }
        Snackbar.make(mGraphicOverlay!!, R.string.permission_camera_rationale, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.ok) { ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM) }
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
                        val zoom = ((scale - minZoom) / (maxZoom - minZoom) * maxCameraZoom).toInt().coerceIn(0, maxCameraZoom)
                        if (params.zoom != zoom) {
                            params.zoom = zoom
                            it.parameters = params
                            Log.d(TAG, "Zoom set to $zoom (scale $scale)")
                        }
                    }
                } catch(e: Exception) {
                    Log.e(TAG, "Failed to set zoom", e)
                }
            } ?: Log.w(TAG, "Could not get camera for zoom")
        }
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

        mCameraSource = CameraSource.Builder(applicationContext, detector)
            .setRequestedPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
            .setFacing(cameraFacing)
                .setAutoFocusEnabled(true)
            .setRequestedFps(PREVIEW_FPS)
                .build()
        
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
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
            R.id.hat, R.id.hat2
        )
    }
}