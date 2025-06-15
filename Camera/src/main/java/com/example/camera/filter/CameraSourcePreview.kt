package com.example.camera.filter

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import java.io.IOException

class CameraSourcePreview(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private val tag = "CameraSourcePreview"
    private val surfaceView: SurfaceView
    private var startRequested = false
    private var surfaceAvailable = false
    private var cameraSource: CameraSource? = null
    private var overlay: GraphicOverlay? = null
    private var aspectRatio = -1.0f // -1 means full screen, > 0 means specific ratio

    init {
        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
        
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) {
            stop()
            return
        }
        
        this.cameraSource = cameraSource
        startRequested = true
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

    fun setAspectRatio(ratio: Float) {
        Log.d(tag, "Setting aspect ratio: $ratio")
        aspectRatio = ratio
        requestLayout()
    }


    @Throws(IOException::class)
    private fun startIfReady() {
        if (startRequested && surfaceAvailable) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CAMERA
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            
            try {
                cameraSource?.start(surfaceView.holder)
                
                if (overlay != null) {
                    val size = cameraSource!!.previewSize
                    val min = Math.min(size.width, size.height)
                    val max = Math.max(size.width, size.height)
                    
                    if (isPortraitMode) {
                        overlay!!.setCameraInfo(min, max, cameraSource!!.cameraFacing)
                    } else {
                        overlay!!.setCameraInfo(max, min, cameraSource!!.cameraFacing)
                    }
                    overlay!!.clear()
                }
                
                startRequested = false
            } catch (e: Exception) {
                Log.e(tag, "Could not start camera source.", e)
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        var previewWidth = width
        var previewHeight = height
        
        if (aspectRatio > 0) {
            // Tính toán kích thước theo aspect ratio cụ thể
            val targetRatio = aspectRatio
            val currentRatio = width.toFloat() / height.toFloat()
            
            if (currentRatio > targetRatio) {
                // Screen is wider than target ratio, adjust width
                previewWidth = (height * targetRatio).toInt()
            } else {
                // Screen is taller than target ratio, adjust height  
                previewHeight = (width / targetRatio).toInt()
            }
            
            Log.d(tag, "Aspect ratio: $targetRatio, Preview size: ${previewWidth}x${previewHeight}")
        } else {
            // Full screen mode
            Log.d(tag, "Full screen mode: ${previewWidth}x${previewHeight}")
        }
        
        setMeasuredDimension(previewWidth, previewHeight)
        
        // Measure all child views
        for (i in 0 until childCount) {
            getChildAt(i).measure(
                MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        
        // Center the preview in the available space
        val parentWidth = (parent as? View)?.width ?: width
        val parentHeight = (parent as? View)?.height ?: height
        
        val offsetX = (parentWidth - width) / 2
        val offsetY = (parentHeight - height) / 2
        
        Log.d(tag, "Layout: ${width}x${height}, offset: (${offsetX}, ${offsetY})")
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, width, height)
        }
        
        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(tag, "Could not start camera source.", e)
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(tag, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    }

    private val isPortraitMode: Boolean
        get() = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT


}

