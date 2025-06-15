package com.example.camera.ui.preview

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

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
    
    init {
        scaleType = ScaleType.MATRIX
    }
    
    fun setScaleFactor(scaleFactor: Float) {
        val drawable = drawable ?: return
        
        // Get drawable dimensions
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        
        // Calculate center point
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Reset matrix and apply scale
        matrix.reset()
        
        // First, center the image
        val scale = if (drawableWidth > drawableHeight) {
            width.toFloat() / drawableWidth
        } else {
            height.toFloat() / drawableHeight
        }
        
        matrix.postTranslate(
            (width - drawableWidth) / 2f,
            (height - drawableHeight) / 2f
        )
        
        // Apply base scale to fit the image
        matrix.postScale(
            scale, scale,
            centerX, centerY
        )
        
        // Apply the additional scale factor
        matrix.postScale(
            scaleFactor, scaleFactor,
            centerX, centerY
        )
        
        imageMatrix = matrix
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
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
                    // Double tap detected
                    setScaleFactor(1.0f) // Reset zoom
                    mode = NONE
                }
                lastTapTime = currentTime
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
                midPoint.set(
                    (event.getX(0) + event.getX(1)) / 2,
                    (event.getY(0) + event.getY(1)) / 2
                )
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(
                        event.x - startPoint.x,
                        event.y - startPoint.y
                    )
                    imageMatrix = matrix
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }
        
        return true
    }
} 