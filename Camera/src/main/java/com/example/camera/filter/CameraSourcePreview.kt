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
    private var aspectRatio = 1.0f
    private var colorFilterOverlay: ColorFilterOverlay

    init {
        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
        
        colorFilterOverlay = ColorFilterOverlay(context)
        addView(colorFilterOverlay)
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
        aspectRatio = ratio
        requestLayout()
    }

    fun setColorFilter(filter: Any?) {
        colorFilterOverlay.applyFilter(filter)
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
        
        if (aspectRatio <= 0) {
            setMeasuredDimension(width, height)
        } else {
            var previewWidth = width
            var previewHeight = (width / aspectRatio).toInt()
            
            if (previewHeight > height) {
                previewHeight = height
                previewWidth = (height * aspectRatio).toInt()
            }
            
            setMeasuredDimension(previewWidth, previewHeight)
        }
        
        for (i in 0 until childCount) {
            getChildAt(i).measure(
                MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val offsetX = (width - child.measuredWidth) / 2
            val offsetY = (height - child.measuredHeight) / 2
            child.layout(offsetX, offsetY, offsetX + child.measuredWidth, offsetY + child.measuredHeight)
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

    private inner class ColorFilterOverlay(context: Context) : View(context) {
        init {
            visibility = GONE
        }
        
        fun applyFilter(filter: Any?) {
            val filterStr = filter?.toString() ?: "NORMAL"
            
            when (filterStr) {
                "BLACK_WHITE" -> {
                    setBackgroundColor(Color.argb(102, 0, 0, 0))
                    visibility = VISIBLE
                }
                "SEPIA" -> {
                    setBackgroundColor(Color.argb(102, 210, 105, 30))
                    visibility = VISIBLE
                }
                "VINTAGE" -> {
                    setBackgroundColor(Color.argb(102, 218, 165, 32))
                    visibility = VISIBLE
                }
                "COOL" -> {
                    setBackgroundColor(Color.argb(102, 65, 105, 225))
                    visibility = VISIBLE
                }
                "WARM" -> {
                    setBackgroundColor(Color.argb(102, 255, 99, 71))
                    visibility = VISIBLE
                }
                else -> {
                    setBackgroundColor(Color.TRANSPARENT)
                    visibility = GONE
                }
            }
            
            bringToFront()
        }
    }
}

