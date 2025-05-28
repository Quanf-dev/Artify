package com.example.imageeditor.tools

import android.graphics.Canvas
import android.view.MotionEvent
import com.example.imageeditor.models.BrushStyle
import com.example.imageeditor.views.PaintCanvasView

interface DrawingTool {
    fun onTouch(event: MotionEvent, canvasView: PaintCanvasView): Boolean
    fun onDraw(canvas: Canvas) // For drawing previews or temporary elements if needed
    fun setBrushStyle(style: BrushStyle)
    fun reset() // Reset tool state if any
} 