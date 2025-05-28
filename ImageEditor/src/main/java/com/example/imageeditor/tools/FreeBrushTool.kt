package com.example.imageeditor.tools

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import com.example.imageeditor.models.BrushStyle
import com.example.imageeditor.views.PaintCanvasView

class FreeBrushTool : DrawingTool {
    private var currentPath: Path = Path()
    private var currentBrushStyle: BrushStyle = BrushStyle()
    private var paint: Paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    init {
        applyBrushStyleToPaint()
    }

    private fun applyBrushStyleToPaint() {
        paint.color = currentBrushStyle.color
        paint.strokeWidth = currentBrushStyle.strokeWidth
        // Apply other style properties if any from currentBrushStyle
    }

    override fun onTouch(event: MotionEvent, canvasView: PaintCanvasView): Boolean {
        val touchX: Float = event.x
        val touchY: Float = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.moveTo(touchX, touchY)
                // Optionally, inform canvasView to prepare for a new path element
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(touchX, touchY)
                // At this point, the canvasView should take currentPath and currentBrushStyle
                // and add it to its list of paths to draw, or draw it onto its internal bitmap.
                // For now, this tool itself doesn't directly draw to the main canvas bitmap.
                // It prepares the path and style.
                canvasView.addPath(currentPath, currentBrushStyle) // This method needs to be added to PaintCanvasView
                currentPath.reset()
            }
            else -> return false
        }
        canvasView.invalidate() // Request a redraw
        return true
    }

    override fun onDraw(canvas: Canvas) {
        // For free brush, the path is drawn directly by PaintCanvasView during onTouchEvent or from its path list.
        // However, this method could be used to draw the currentPath in real-time if PaintCanvasView delegates drawing of active tool path here.
        canvas.drawPath(currentPath, paint)
    }

    override fun setBrushStyle(style: BrushStyle) {
        currentBrushStyle = style
        applyBrushStyleToPaint()
    }

    override fun reset() {
        currentPath.reset()
    }
} 