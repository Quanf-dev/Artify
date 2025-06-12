package com.example.camera.filter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import java.io.IOException

class CameraSourcePreview(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private val tag = "CameraSourcePreview"
    private val surfaceView: SurfaceView
    private var isStartRequested: Boolean = false
    private var isSurfaceAvailable: Boolean = false
    private var cameraSource: CameraSource? = null
    private var overlay: GraphicOverlay? = null
    private var aspectRatio: Float = 4f / 3f
    
    // Zoom functionality
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var scaleFactor: Float = 1.0f
    private val maxZoom: Float = 3.0f
    private val minZoom: Float = 1.0f

    init {
        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
        
        setupZoomGesture()
    }
    
    private fun setupZoomGesture() {
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(minZoom, maxZoom)
                
                // Apply zoom - this would need camera API support
                Log.d(tag, "Zoom factor: $scaleFactor")
                return true
            }
        })
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector?.onTouchEvent(event)
        return true
    }

    fun setAspectRatio(ratio: Float) {
        if (ratio <= 0) return
        aspectRatio = ratio
        requestLayout() // Force a re-layout
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) {
            stop()
            return
        }

        this.cameraSource = cameraSource
        isStartRequested = true
        startIfReady()
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?, overlay: GraphicOverlay?) {
        this.overlay = overlay
        start(cameraSource)
    }

    fun stop() {
        cameraSource?.stop()
    }

    fun release() {
        cameraSource?.release()
        cameraSource = null
    }

    @Throws(IOException::class)
    private fun startIfReady() {
        if (isStartRequested && isSurfaceAvailable) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            
            try {
                Log.d(tag, "Starting camera source")
                cameraSource?.start(surfaceView.holder)
                
                cameraSource?.let { source ->
                    val size = source.previewSize
                    val min = kotlin.math.min(size.width, size.height)
                    val max = kotlin.math.max(size.width, size.height)
                    
                    overlay?.let { overlay ->
                        if (isPortraitMode) {
                            // Swap width and height sizes when in portrait
                            overlay.setCameraInfo(min, max, source.cameraFacing)
                        } else {
                            overlay.setCameraInfo(max, min, source.cameraFacing)
                        }
                        overlay.clear()
                    }
                }
                
                isStartRequested = false
                Log.d(tag, "Camera source started successfully")
            } catch (e: Exception) {
                Log.e(tag, "Could not start camera source.", e)
            }
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(tag, "Surface created")
            isSurfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(tag, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.d(tag, "Surface destroyed")
            isSurfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.d(tag, "Surface changed: $width x $height")
            // No specific action needed
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val parentWidth = (right - left)
        val parentHeight = (bottom - top)

        var previewWidth = parentWidth
        var previewHeight = (previewWidth / aspectRatio).toInt()

        if (previewHeight > parentHeight) {
            previewHeight = parentHeight
            previewWidth = (previewHeight * aspectRatio).toInt()
        }

        val childLeft = (parentWidth - previewWidth) / 2
        val childTop = (parentHeight - previewHeight) / 2
        val childRight = childLeft + previewWidth
        val childBottom = childTop + previewHeight

        Log.d(tag, "Layout preview to: $childLeft, $childTop, $childRight, $childBottom")

        for (i in 0 until childCount) {
            getChildAt(i).layout(childLeft, childTop, childRight, childBottom)
        }

        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(tag, "Could not start camera source.", e)
        }
    }

    private val isPortraitMode: Boolean
        get() {
            val orientation = context.resources.configuration.orientation
            return orientation == Configuration.ORIENTATION_PORTRAIT
        }
}
