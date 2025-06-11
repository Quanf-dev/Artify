package com.example.camera.ui.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.camera.domain.model.FilterType
import timber.log.Timber

class FilterEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentFilter: FilterType = FilterType.NONE
    private val filterPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }

    fun setFilter(filter: FilterType) {
        if (currentFilter != filter) {
            currentFilter = filter
            Timber.d("Filter changed to: $filter")
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (currentFilter == FilterType.NONE) return

        // Apply filter overlay based on filter type
        when (currentFilter) {
            FilterType.SEPIA -> drawSepiaFilter(canvas)
            FilterType.BLACK_WHITE -> drawBlackWhiteFilter(canvas)
            FilterType.CINEMATIC -> drawCinematicFilter(canvas)
            FilterType.VINTAGE -> drawVintageFilter(canvas)
            FilterType.COLD -> drawColdFilter(canvas)
            FilterType.WARM -> drawWarmFilter(canvas)
            FilterType.NONE -> { /* No filter */ }
        }
    }

    private fun drawSepiaFilter(canvas: Canvas) {
        // Apply sepia overlay
        filterPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0.5f) // Desaturate
            postConcat(ColorMatrix(floatArrayOf(
                1.0f, 0.0f, 0.0f, 0.0f, 40f,  // Red
                0.0f, 1.0f, 0.0f, 0.0f, 20f,  // Green
                0.0f, 0.0f, 1.0f, 0.0f, -10f, // Blue
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f  // Alpha
            )))
        })
        
        // Draw sepia overlay
        filterPaint.color = Color.argb(30, 139, 69, 19) // Sepia brown overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
    }

    private fun drawBlackWhiteFilter(canvas: Canvas) {
        // Apply grayscale effect
        filterPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0f) // Complete desaturation
        })
        
        // Draw slight contrast overlay
        filterPaint.color = Color.argb(20, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
    }

    private fun drawCinematicFilter(canvas: Canvas) {
        // Apply cinematic color grading
        filterPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            1.2f, 0.0f, 0.0f, 0.0f, -10f,  // Red enhanced
            0.0f, 1.1f, 0.0f, 0.0f, 0f,    // Green slightly enhanced
            0.0f, 0.0f, 0.8f, 0.0f, 10f,   // Blue reduced
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f   // Alpha
        )))
        
        // Add film grain effect with dark edges
        filterPaint.color = Color.argb(25, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
        
        // Add vignette effect
        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f,
                kotlin.math.max(width, height) / 2f,
                Color.TRANSPARENT,
                Color.argb(60, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }

    private fun drawVintageFilter(canvas: Canvas) {
        // Apply vintage color effect
        filterPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0.7f) // Slightly desaturated
            postConcat(ColorMatrix(floatArrayOf(
                1.1f, 0.1f, 0.0f, 0.0f, 20f,  // Red enhanced with cross-channel
                0.0f, 1.0f, 0.1f, 0.0f, 10f,  // Green with yellow tint
                0.0f, 0.0f, 0.9f, 0.0f, -5f,  // Blue reduced
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f  // Alpha
            )))
        })
        
        // Add vintage yellow overlay
        filterPaint.color = Color.argb(35, 255, 204, 102) // Vintage yellow
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
    }

    private fun drawColdFilter(canvas: Canvas) {
        // Apply cold tone effect
        filterPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0.9f, 0.0f, 0.1f, 0.0f, -5f,   // Red reduced, blue tint
            0.0f, 1.0f, 0.1f, 0.0f, 5f,    // Green with blue tint
            0.1f, 0.1f, 1.2f, 0.0f, 15f,   // Blue enhanced
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f   // Alpha
        )))
        
        // Add cold blue overlay
        filterPaint.color = Color.argb(30, 102, 178, 255) // Cold blue
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
    }

    private fun drawWarmFilter(canvas: Canvas) {
        // Apply warm tone effect
        filterPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            1.2f, 0.1f, 0.0f, 0.0f, 15f,   // Red enhanced
            0.1f, 1.1f, 0.0f, 0.0f, 10f,   // Green enhanced with red tint
            0.0f, 0.0f, 0.8f, 0.0f, -10f,  // Blue reduced
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f   // Alpha
        )))
        
        // Add warm orange overlay
        filterPaint.color = Color.argb(25, 255, 140, 60) // Warm orange
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
    }

    fun clearFilter() {
        setFilter(FilterType.NONE)
    }
} 