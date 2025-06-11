package com.example.camera.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import timber.log.Timber

class GridLinesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var isGridVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                Timber.d("Grid visibility changed to: $value")
                visibility = if (value) VISIBLE else GONE
                invalidate()
            }
        }

    private val gridPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 180 // Semi-transparent
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isGridVisible) return

        drawRuleOfThirdsGrid(canvas)
    }

    private fun drawRuleOfThirdsGrid(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0 || height <= 0) return

        // Vertical lines (divide width into thirds)
        val verticalLine1 = width / 3f
        val verticalLine2 = width * 2f / 3f

        canvas.drawLine(verticalLine1, 0f, verticalLine1, height, gridPaint)
        canvas.drawLine(verticalLine2, 0f, verticalLine2, height, gridPaint)

        // Horizontal lines (divide height into thirds)
        val horizontalLine1 = height / 3f
        val horizontalLine2 = height * 2f / 3f

        canvas.drawLine(0f, horizontalLine1, width, horizontalLine1, gridPaint)
        canvas.drawLine(0f, horizontalLine2, width, horizontalLine2, gridPaint)
    }

    fun toggleGrid() {
        isGridVisible = !isGridVisible
    }

    fun showGrid() {
        isGridVisible = true
    }

    fun hideGrid() {
        isGridVisible = false
    }
} 