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
import android.graphics.Camera
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
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

    private val observer: Thread = object : Thread("observer") {
        init {
            isDaemon = true
        }

        override fun run() {
            while (!isInterrupted()) {
                /*
                TextGraphic mTextGraphic = new TextGraphic(mGraphicOverlay);
                mGraphicOverlay.add(mTextGraphic);*/
                //mTextGraphic.updateText(2);
            }
        }
    }

    private var mCameraSource: CameraSource? = null
    private var typeFace = 0
    private val typeFlash = 0
    private var flashmode = false
    private val camera: Camera? = null

    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay? = null

    //==============================================================================================
    // Activity Methods
    //==============================================================================================
    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_face_filter)

        mPreview = findViewById(R.id.preview) as CameraSourcePreview
        mGraphicOverlay = findViewById(R.id.faceOverlay) as GraphicOverlay

        // Check for the camera permission before accessing the camera.
        val rc: Int = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission already granted, creating camera source")
            createCameraSource()
        } else {
            Log.d(TAG, "Camera permission not granted, requesting permission")
            requestCameraPermission()
        }

        // Set up UI elements
        setupUI()
    }

    private fun setupUI() {
        //mTextGraphic = new TextGraphic(mGraphicOverlay);
        //mGraphicOverlay.add(mTextGraphic);
        val face: ImageButton = findViewById(R.id.face) as ImageButton
        face.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (findViewById<View>(R.id.scrollView).visibility == View.GONE) {
                    findViewById<View>(R.id.scrollView).visibility = View.VISIBLE
                    (findViewById(R.id.face) as ImageButton).setImageResource(R.drawable.face_select)
                } else {
                    findViewById<View>(R.id.scrollView).visibility = View.GONE
                    (findViewById(R.id.face) as ImageButton).setImageResource(R.drawable.face)
                }
            }
        })

        val no_filter: ImageButton = findViewById(R.id.no_filter) as ImageButton
        no_filter.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 0
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val hair: ImageButton = findViewById(R.id.hair) as ImageButton
        hair.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 1
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val op: ImageButton = findViewById(R.id.op) as ImageButton
        op.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 2
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val snap: ImageButton = findViewById(R.id.snap) as ImageButton
        snap.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 3
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val glasses2: ImageButton = findViewById(R.id.glasses2) as ImageButton
        glasses2.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 4
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val glasses3: ImageButton = findViewById(R.id.glasses3) as ImageButton
        glasses3.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 5
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val glasses4: ImageButton = findViewById(R.id.glasses4) as ImageButton
        glasses4.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 6
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val glasses5: ImageButton = findViewById(R.id.glasses5) as ImageButton
        glasses5.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 7
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })


        val mask2: ImageButton = findViewById(R.id.mask2) as ImageButton
        mask2.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 8
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val mask3: ImageButton = findViewById(R.id.mask3) as ImageButton
        mask3.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 9
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val dog: ImageButton = findViewById(R.id.dog) as ImageButton
        dog.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 10
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val cat2: ImageButton = findViewById(R.id.cat2) as ImageButton
        cat2.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
                typeFace = 11
                findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
                refreshFaceDetection()
            }
        })

        val hat: ImageButton = findViewById(R.id.hat) as ImageButton
        hat.setOnClickListener {
            findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
            typeFace = 12
            findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
            refreshFaceDetection()
        }

        val hat2: ImageButton = findViewById(R.id.hat2) as ImageButton
        hat2.setOnClickListener {
            findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background)
            typeFace = 13
            findViewById<View>(MASK[typeFace]).background = ContextCompat.getDrawable(this@FaceFilterActivity, R.drawable.round_background_select)
            refreshFaceDetection()
        }

        val button: ImageButton = findViewById(R.id.change) as ImageButton
        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
            }
        })

        val flash: ImageButton = findViewById(R.id.flash) as ImageButton
        flash.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val blue: Int
                if (flashmode == false) {
                    flashmode = true
                    blue = 0
                } else {
                    flashmode = false
                    blue = 255
                }
                flash.setColorFilter(Color.argb(255, 255, 255, blue))
            }
        })

        val camera: ImageButton = findViewById(R.id.camera) as ImageButton
        camera.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                //takeImage();
                onPause()
            }
        })
    }

    private fun takeImage() {
        try {
            //openCamera(CameraInfo.CAMERA_FACING_BACK);
            //releaseCameraSource();
            //releaseCamera();
            //openCamera(CameraInfo.CAMERA_FACING_BACK);
            //setUpCamera(camera);
            //Thread.sleep(1000);
            val localCameraSource = mCameraSource
            localCameraSource?.takePicture(null, object : CameraSource.PictureCallback {
                private var imageFile: File? = null
                override fun onPictureTaken(bytes: ByteArray) {
                    try {
                        // convert byte array into bitmap
                        var loadedImage: Bitmap? = null
                        var rotatedBitmap: Bitmap? = null
                        loadedImage = BitmapFactory.decodeByteArray(
                            bytes, 0,
                            bytes.size
                        )

                        // rotate Image
                        val rotateMatrix = Matrix()
                        rotateMatrix.postRotate(
                            windowManager.defaultDisplay.rotation.toFloat()
                        )
                        rotatedBitmap = Bitmap.createBitmap(
                            loadedImage!!, 0, 0,
                            loadedImage.width, loadedImage.height,
                            rotateMatrix, false
                        )
                        val state = Environment.getExternalStorageState()
                        var folder: File? = null
                        if (state.contains(Environment.MEDIA_MOUNTED)) {
                            folder = File(
                                Environment
                                    .getExternalStorageDirectory().toString() + "/faceFilter"
                            )
                        } else {
                            folder = File(
                                Environment
                                    .getExternalStorageDirectory().toString() + "/faceFilter"
                            )
                        }

                        var success = true
                        if (!folder.exists()) {
                            success = folder.mkdirs()
                        }
                        if (success) {
                            val date = Date()
                            imageFile = File(
                                (folder.absolutePath
                                        + File.separator //+ new Timestamp(date.getTime()).toString()
                                        + "Image.jpg")
                            )

                            imageFile!!.createNewFile()
                        } else {
                            Toast.makeText(
                                baseContext, "Image Not saved",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        val ostream = ByteArrayOutputStream()

                        // save image into gallery
                        rotatedBitmap = resize(rotatedBitmap, 800, 600)
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream)

                        val fout = FileOutputStream(imageFile)
                        fout.write(ostream.toByteArray())
                        fout.close()
                        val values: ContentValues = ContentValues()

                        values.put(
                            MediaStore.Images.Media.DATE_TAKEN,
                            System.currentTimeMillis()
                        )
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        values.put(
                            MediaStore.MediaColumns.DATA,
                            imageFile!!.absolutePath
                        )

                        setResult(Activity.RESULT_OK) //add this
                        finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
        } catch (ex: Exception) {
        }
    }

    private fun resize(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var image = image
        if (maxHeight > 0 && maxWidth > 0) {
            val width = image.width
            val height = image.height
            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > 1) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
            return image
        } else {
            return image
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            Log.d(TAG, "Requesting permission directly")
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        Log.d(TAG, "Showing permission rationale")
        val thisActivity: Activity = this

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(
                thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM
            )
        }

        mGraphicOverlay?.let {
            Snackbar.make(
                it, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok, listener)
                .show()
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private fun createCameraSource() {
        Log.d(TAG, "Creating camera source")
        val context: Context = applicationContext
        val detector = FaceDetector.Builder(context)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .setProminentFaceOnly(true)
            .setTrackingEnabled(true)
            .setMode(FaceDetector.ACCURATE_MODE)
            .setMinFaceSize(0.15f)
            .build()

        detector.setProcessor(
            MultiProcessor.Builder<Face?>(GraphicFaceTrackerFactory())
                .build()
        )

        if (!detector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.")
            val lowStorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, lowStorageFilter) != null
            
            if (hasLowStorage) {
                Toast.makeText(this, "Face detection requires more storage space", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Face detector - low storage")
            }
        }

        try {
            mCameraSource = CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480) // Use lower resolution to avoid black screen
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(30.0f)
                .build()
            Log.d(TAG, "Camera source created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating camera source: ${e.message}", e)
            Toast.makeText(this, "Could not create camera source", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        
        // Check for the camera permission before accessing the camera.
        val rc: Int = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            // Make sure camera source is created
            if (mCameraSource == null) {
                createCameraSource()
            }
            startCameraSource()
        } else {
            Log.w(TAG, "Camera permission not granted, requesting")
            requestCameraPermission()
        }
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        mPreview?.stop()
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        mPreview?.stop()
        mCameraSource?.release()
        mCameraSource = null
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [.requestPermissions].
     *
     *
     * **Note:** It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     *
     * @param requestCode  The request code passed in [.requestPermissions].
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never null.
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - creating camera source")
            // We have permission, so create the camera source
            createCameraSource()
            return
        }

        Log.e(
            TAG, "Permission not granted: results len = " + grantResults.size +
                    " Result code = " + (if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")
        )

        val listener = DialogInterface.OnClickListener { _, _ ->
            finish()
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Face Filter")
            .setMessage(R.string.no_camera_permission)
            .setPositiveButton(R.string.ok, listener)
            .show()
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================
    /**
     * Starts or restarts the camera source, if it exists.
     */
    private fun startCameraSource() {
        Log.d(TAG, "Starting camera source")
        
        // Check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            applicationContext
        )
        if (code != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available: $code")
            val dlg =
                GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg?.show()
            return
        }

        mCameraSource?.let { cameraSource ->
            try {
                Log.d(TAG, "Starting camera preview")
                mPreview?.start(cameraSource, mGraphicOverlay)
                Log.d(TAG, "Camera preview started")
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source: ${e.message}", e)
                cameraSource.release()
                mCameraSource = null
                Toast.makeText(this, "Could not start camera", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera source: ${e.message}", e)
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.e(TAG, "Camera source is null")
            Toast.makeText(this, "Camera not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class GraphicTextTrackerFactory : MultiProcessor.Factory<String?> {
        override fun create(face: String?): Tracker<String?> {
            return GraphicTextTracker(mGraphicOverlay!!)
        }
    }

    private inner class GraphicTextTracker(private val mOverlay: GraphicOverlay) :
        Tracker<String?>() {
        private val mTextGraphic: TextGraphic

        init {
            mTextGraphic = TextGraphic(mOverlay)
        }

        // This is not an override method, just a regular method
        fun onUpdate() {
            mOverlay.add(mTextGraphic)
            mTextGraphic.updateText(3)
        }

        override fun onDone() {
            mOverlay.remove(mTextGraphic)
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================
    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private inner class GraphicFaceTrackerFactory : MultiProcessor.Factory<Face?> {
        override fun create(face: Face?): Tracker<Face?> {
            return GraphicFaceTracker(mGraphicOverlay!!)
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private inner class GraphicFaceTracker(private val mOverlay: GraphicOverlay) :
        Tracker<Face?>() {
        private val mFaceGraphic: FaceGraphic

        init {
            mFaceGraphic = FaceGraphic(mOverlay, typeFace)
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        override fun onNewItem(faceId: Int, item: Face?) {
            mFaceGraphic.setId(faceId)
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        override fun onUpdate(detectionResults: Detector.Detections<Face?>, face: Face?) {
            face?.let {
                mOverlay.add(mFaceGraphic)
                mFaceGraphic.updateFace(it, typeFace)
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        override fun onMissing(detectionResults: Detector.Detections<Face?>) {
            mOverlay.remove(mFaceGraphic)
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        override fun onDone() {
            mOverlay.remove(mFaceGraphic)
        }
    }

    /**
     * Helper method to refresh face detection when filter type changes
     */
    private fun refreshFaceDetection() {
        mGraphicOverlay?.clear()
    }

    companion object {
        private const val TAG = "FaceTracker"

        private val MASK = intArrayOf(
            R.id.no_filter,
            R.id.hair,
            R.id.op,
            R.id.snap,
            R.id.glasses2,
            R.id.glasses3,
            R.id.glasses4,
            R.id.glasses5,
            R.id.mask2,
            R.id.mask3,
            R.id.dog,
            R.id.cat2,
            R.id.hat,
            R.id.hat2
        )

        private const val RC_HANDLE_GMS = 9001

        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
    }
}