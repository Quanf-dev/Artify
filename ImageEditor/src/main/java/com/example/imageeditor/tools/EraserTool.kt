package com.example.imageeditor.tools

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import com.example.imageeditor.models.BrushStyle
import com.example.imageeditor.views.PaintCanvasView

class EraserTool : DrawingTool {
    private var currentPath: Path = Path()
    // Eraser uses a fixed color (usually transparent or background) but respects stroke width
    private var currentBrushStyle: BrushStyle = BrushStyle(color = Color.TRANSPARENT, strokeWidth = 20f) // Default eraser size
    private var eraserPaint: Paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) // Key for erasing
    }

    init {
        applyBrushStyleToPaint()
    }

    private fun applyBrushStyleToPaint() {
        // Eraser color is fixed (transparent via PorterDuff.Mode.CLEAR), but width can change
        eraserPaint.strokeWidth = currentBrushStyle.strokeWidth
        // Do not set color on eraserPaint, PorterDuff.Mode.CLEAR handles transparency
    }

    override fun onTouch(event: MotionEvent, canvasView: PaintCanvasView): Boolean {
        val touchX: Float = event.x
        val touchY: Float = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(touchX, touchY)
                // Create a new BrushStyle for the eraser action, specifically for recording or drawing
                // The color here doesn't matter for the eraserPaint due to Xfermode, but might be useful for history.
                val eraserDrawStyle = BrushStyle(color = Color.TRANSPARENT, strokeWidth = currentBrushStyle.strokeWidth)
                canvasView.addPath(currentPath, eraserDrawStyle, true) // `isEraser` flag might be needed in addPath
                currentPath.reset()
            }
            else -> return false
        }
        canvasView.invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        // This could draw the current erasing path if needed for preview
        canvas.drawPath(currentPath, eraserPaint)
    }

    override fun setBrushStyle(style: BrushStyle) {
        // Eraser mainly cares about strokeWidth. Color is effectively ignored due to Xfermode.
        currentBrushStyle = style.copy(color = Color.TRANSPARENT) // Ensure color is consistent for eraser logic
        applyBrushStyleToPaint()
    }

    fun setStrokeWidth(width: Float) {
        currentBrushStyle = currentBrushStyle.copy(strokeWidth = width)
        applyBrushStyleToPaint()
    }

    override fun reset() {
        currentPath.reset()
    }
} 