package com.example.imageeditor.tools.shapes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import com.example.imageeditor.tools.base.BaseTool
import com.example.imageeditor.tools.base.DrawableItem
import java.util.UUID
import kotlin.math.min
import kotlin.math.max

class RectangleTool : BaseTool() {
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var currentX: Float = 0f
    private var currentY: Float = 0f
    private var isDrawing: Boolean = false
    private var currentDrawable: RectangleDrawable? = null
    
    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private var isFilled: Boolean = false
    
    override fun onTouchStart(event: MotionEvent) {
        startX = event.x
        startY = event.y
        currentX = startX
        currentY = startY
        isDrawing = true
        
        // Create drawable immediately with current properties
        val path = Path()
        currentDrawable = RectangleDrawable(
            path = path,
            color = paint.color,
            strokeWidth = paint.strokeWidth,
            opacity = paint.alpha,
            isFilled = isFilled
        )
        updateDrawablePath()
    }
    
    override fun onTouchMove(event: MotionEvent) {
        if (!isDrawing) return
        currentX = event.x
        currentY = event.y
        updateDrawablePath()
    }
    
    override fun onTouchEnd(event: MotionEvent) {
        if (!isDrawing) return
        isDrawing = false
        updateDrawablePath()
    }
    
    private fun updateDrawablePath() {
        val rect = RectF(
            min(startX, currentX),
            min(startY, currentY),
            max(startX, currentX),
            max(startY, currentY)
        )
        
        currentDrawable?.updateRect(rect)
    }
    
    override fun drawPreview(canvas: Canvas) {
        if (!isDrawing) return
        currentDrawable?.draw(canvas, 255)
    }
    
    override fun createDrawableItem(): DrawableItem? {
        if (!isDrawing) return null
        val result = currentDrawable
        currentDrawable = null
        return result
    }
    
    override fun getSelectedItem(): DrawableItem? = currentDrawable
    
    override fun onToolDeselected() {
        currentDrawable = null
    }
    
    fun setColor(color: Int) {
        paint.color = color
    }
    
    fun setStrokeWidth(width: Float) {
        paint.strokeWidth = width
    }
    
    fun setOpacity(opacity: Int) {
        paint.alpha = opacity
    }
    
    fun setFillMode(filled: Boolean) {
        isFilled = filled
    }
}

class RectangleDrawable(
    private val path: Path,
    override var color: Int,
    override var strokeWidth: Float,
    override var opacity: Int,
    override var isFilled: Boolean
) : DrawableItem {
    override val id: String = UUID.randomUUID().toString()
    override var position: PointF = PointF(0f, 0f)
    override var rotation: Float = 0f
    
    private val paint = Paint().apply {
        this.color = color
        this.strokeWidth = strokeWidth
        this.isAntiAlias = true
    }
    
    fun updateRect(rect: RectF) {
        path.reset()
        path.addRect(rect, Path.Direction.CW)
    }
    
    override fun draw(canvas: Canvas, layerOpacity: Int) {
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.alpha = (opacity * layerOpacity / 255f).toInt()
        paint.style = if (isFilled) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
        
        canvas.save()
        val bounds = getBounds()
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        canvas.translate(position.x, position.y)
        canvas.rotate(rotation, centerX, centerY)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
    
    override fun contains(x: Float, y: Float): Boolean {
        val bounds = getBounds()
        return bounds.contains(x, y)
    }
    
    override fun getBounds(): RectF {
        val bounds = RectF()
        val matrix = Matrix()
        matrix.setTranslate(position.x, position.y)
        val tempPath = Path(path)
        tempPath.transform(matrix)
        val tempBounds = RectF()
        tempPath.computeBounds(tempBounds, true)
        bounds.set(tempBounds.left, tempBounds.top, tempBounds.right, tempBounds.bottom)
        return bounds
    }
    
    override fun transform(matrix: Matrix) {
        path.transform(matrix)
    }
    
    override fun copy(): DrawableItem {
        return RectangleDrawable(
            Path(path),
            color,
            strokeWidth,
            opacity,
            isFilled
        )
    }
} 