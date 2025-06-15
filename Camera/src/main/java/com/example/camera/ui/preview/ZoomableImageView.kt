package com.example.camera.ui.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val TAG = "ZoomableImageView"
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private val midPoint = PointF()
    
    // States for touch handling
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var mode = NONE
    
    // For double tap detection
    private var lastTapTime: Long = 0
    private val doubleTapTimeout = 300L
    
    // Zoom limits
    private val minScale = 0.5f
    private val maxScale = 3.0f
    private var currentScale = 1.0f
    
    // Color filter support
    private var colorFilterPaint: Paint? = null
    
    init {
        scaleType = ScaleType.MATRIX
        Log.d(TAG, "ZoomableImageView initialized")
    }
    
    /**
     * Set color filter for the image view
     */

    override fun onDraw(canvas: Canvas) {
        if (canvas == null) return
        
        // Apply color filter if set
        colorFilterPaint?.let { filterPaint ->
            val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
            super.onDraw(canvas)
            canvas.restoreToCount(layerId)
        } ?: run {
            super.onDraw(canvas)
        }
    }
    
    fun setScaleFactor(scaleFactor: Float) {
        val drawable = drawable ?: return
        
        Log.d(TAG, "Setting scale factor: $scaleFactor")
        
        // Get drawable dimensions
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            Log.w(TAG, "Invalid drawable dimensions: ${drawableWidth}x${drawableHeight}")
            return
        }
        
        // Calculate center point
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Reset matrix
        matrix.reset()
        
        // Calculate initial scale to fit image properly
        val scaleX = width.toFloat() / drawableWidth.toFloat()
        val scaleY = height.toFloat() / drawableHeight.toFloat()
        
        // Use the smaller scale to ensure the image fits completely
        val baseScale = minOf(scaleX, scaleY)
        
        // Calculate translation to center the image
        val translationX = (width - drawableWidth * baseScale) / 2f
        val translationY = (height - drawableHeight * baseScale) / 2f
        
        // Apply transformations
        matrix.postScale(baseScale, baseScale)
        matrix.postTranslate(translationX, translationY)
        
        // Apply the additional scale factor
        val finalScale = baseScale * scaleFactor.coerceIn(minScale, maxScale)
        currentScale = finalScale / baseScale
        
        if (scaleFactor != 1.0f) {
            matrix.postScale(scaleFactor, scaleFactor, centerX, centerY)
        }
        
        imageMatrix = matrix
        Log.d(TAG, "Applied scale: base=$baseScale, factor=$scaleFactor, final=$finalScale")
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "Size changed: ${w}x${h}")
        if (drawable != null) {
            setScaleFactor(1.0f)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                startPoint.set(event.x, event.y)
                mode = DRAG
                
                // Check for double tap
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < doubleTapTimeout) {
                    // Double tap detected - reset zoom
                    Log.d(TAG, "Double tap detected - resetting zoom")
                    setScaleFactor(1.0f)
                    mode = NONE
                    return true
                }
                lastTapTime = currentTime
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
                midPoint.set(
                    (event.getX(0) + event.getX(1)) / 2,
                    (event.getY(0) + event.getY(1)) / 2
                )
                savedMatrix.set(matrix)
                Log.d(TAG, "Zoom mode started")
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    // Handle dragging
                    matrix.set(savedMatrix)
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    matrix.postTranslate(dx, dy)
                    
                    // Constrain translation to keep image visible
                    constrainTranslation()
                    imageMatrix = matrix
                    
                } else if (mode == ZOOM && event.pointerCount == 2) {
                    // Handle zooming
                    val newDist = getDistance(event)
                    val oldDist = getDistance(event) // This should be calculated from saved points
                    
                    if (newDist > 10f && oldDist > 10f) {
                        val scale = newDist / oldDist
                        matrix.set(savedMatrix)
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y)
                        
                        // Constrain scale
                        constrainScale()
                        imageMatrix = matrix
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                Log.d(TAG, "Touch ended")
            }
        }
        
        return true
    }
    
    private fun getDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }
    
    private fun constrainTranslation() {
        val drawable = drawable ?: return
        
        val values = FloatArray(9)
        matrix.getValues(values)
        
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        
        val drawableWidth = drawable.intrinsicWidth * scaleX
        val drawableHeight = drawable.intrinsicHeight * scaleY
        
        var deltaX = 0f
        var deltaY = 0f
        
        // Check horizontal bounds
        if (drawableWidth <= width) {
            // Image is smaller than view, center it
            deltaX = (width - drawableWidth) / 2f - transX
        } else {
            // Image is larger than view, constrain to edges
            if (transX > 0) deltaX = -transX
            else if (transX + drawableWidth < width) deltaX = width - drawableWidth - transX
        }
        
        // Check vertical bounds
        if (drawableHeight <= height) {
            // Image is smaller than view, center it
            deltaY = (height - drawableHeight) / 2f - transY
        } else {
            // Image is larger than view, constrain to edges
            if (transY > 0) deltaY = -transY
            else if (transY + drawableHeight < height) deltaY = height - drawableHeight - transY
        }
        
        matrix.postTranslate(deltaX, deltaY)
    }
    
    private fun constrainScale() {
        val values = FloatArray(9)
        matrix.getValues(values)
        
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        
        val scale = minOf(scaleX, scaleY)
        
        if (scale < minScale || scale > maxScale) {
            val constrainedScale = scale.coerceIn(minScale, maxScale)
            val scaleFactor = constrainedScale / scale
            
            matrix.postScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
            Log.d(TAG, "Scale constrained from $scale to $constrainedScale")
        }
    }
} 