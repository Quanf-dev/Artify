package com.example.imageeditor.tools

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent
import com.example.imageeditor.models.BrushStyle
import com.example.imageeditor.views.PaintCanvasView

enum class ShapeType {
    LINE, RECTANGLE, OVAL // Add more shapes as needed
}

class ShapeTool(private var shapeType: ShapeType = ShapeType.LINE) : DrawingTool {
    private var startPoint: PointF? = null
    private var currentShapePath: Path = Path()
    private var currentBrushStyle: BrushStyle = BrushStyle()
    private var paint: Paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE // Shapes can be STROKE or FILL_AND_STROKE
    }

    init {
        applyBrushStyleToPaint()
    }

    private fun applyBrushStyleToPaint() {
        paint.color = currentBrushStyle.color
        paint.strokeWidth = currentBrushStyle.strokeWidth
        // Potentially set paint.style (FILL, STROKE, FILL_AND_STROKE) based on BrushStyle property
    }

    fun setShapeType(type: ShapeType) {
        shapeType = type
    }

    override fun onTouch(event: MotionEvent, canvasView: PaintCanvasView): Boolean {
        val touchX: Float = event.x
        val touchY: Float = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startPoint = PointF(touchX, touchY)
                currentShapePath.reset()
                // For shapes like rectangles or ovals, you might start drawing a preview path here
            }
            MotionEvent.ACTION_MOVE -> {
                startPoint?.let {
                    currentShapePath.reset()
                    when (shapeType) {
                        ShapeType.LINE -> {
                            currentShapePath.moveTo(it.x, it.y)
                            currentShapePath.lineTo(touchX, touchY)
                        }
                        ShapeType.RECTANGLE -> {
                            currentShapePath.addRect(it.x, it.y, touchX, touchY, Path.Direction.CW)
                        }
                        ShapeType.OVAL -> {
                            // Path.addOval requires API 21
                            currentShapePath.addOval(it.x, it.y, touchX, touchY, Path.Direction.CW)
                        }
                    }
                    canvasView.invalidate() // Redraw to show preview
                }
            }
            MotionEvent.ACTION_UP -> {
                startPoint?.let {
                    // Finalize the shape path based on current touchX, touchY
                    currentShapePath.reset() // Reset and rebuild for final draw to avoid artifacts
                    when (shapeType) {
                        ShapeType.LINE -> {
                            currentShapePath.moveTo(it.x, it.y)
                            currentShapePath.lineTo(touchX, touchY)
                        }
                        ShapeType.RECTANGLE -> {
                            currentShapePath.addRect(it.x, it.y, touchX, touchY, Path.Direction.CW)
                        }
                        ShapeType.OVAL -> {
                            currentShapePath.addOval(it.x, it.y, touchX, touchY, Path.Direction.CW)
                        }
                    }
                    canvasView.addPath(currentShapePath, currentBrushStyle)
                    currentShapePath.reset()
                    startPoint = null
                    canvasView.invalidate()
                }
            }
            else -> return false
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        // This method is crucial for ShapeTool to draw the shape preview during ACTION_MOVE
        if (startPoint != null) { // Only draw if a shape drawing process is active
            canvas.drawPath(currentShapePath, paint)
        }
    }

    override fun setBrushStyle(style: BrushStyle) {
        currentBrushStyle = style
        applyBrushStyleToPaint()
    }

    override fun reset() {
        startPoint = null
        currentShapePath.reset()
    }
} 