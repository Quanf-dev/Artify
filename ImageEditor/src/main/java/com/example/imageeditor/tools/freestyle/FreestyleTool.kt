package com.example.imageeditor.tools.freestyle

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
import kotlin.math.abs

class FreestyleTool : BaseTool() {
    private var currentPath: Path? = null
    private var currentDrawable: FreestyleDrawable? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    
    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    override fun onTouchStart(event: MotionEvent) {
        currentPath = Path()
        lastX = event.x
        lastY = event.y
        currentPath?.moveTo(event.x, event.y)
        currentDrawable = FreestyleDrawable(
            path = Path(),
            color = paint.color,
            strokeWidth = paint.strokeWidth,
            opacity = paint.alpha
        )
    }
    
    override fun onTouchMove(event: MotionEvent) {
        val dx = abs(event.x - lastX)
        val dy = abs(event.y - lastY)
        
        if (dx >= 3f || dy >= 3f) {
            // Implement quadratic bezier curve for smooth drawing
            currentPath?.quadTo(
                lastX, 
                lastY,
                (event.x + lastX) / 2,
                (event.y + lastY) / 2
            )
            currentDrawable?.addToPath(currentPath!!)
            lastX = event.x
            lastY = event.y
        }
    }
    
    override fun onTouchEnd(event: MotionEvent) {
        currentPath?.lineTo(event.x, event.y)
        currentDrawable?.addToPath(currentPath!!)
    }
    
    override fun drawPreview(canvas: Canvas) {
        currentPath?.let { canvas.drawPath(it, paint) }
    }
    
    override fun createDrawableItem(): DrawableItem? {
        if (currentPath == null || currentPath!!.isEmpty) {
            return null
        }
        val result = currentDrawable
        currentDrawable = null
        currentPath = null
        return result
    }
    
    override fun getSelectedItem(): DrawableItem? = currentDrawable
    
    override fun onToolDeselected() {
        currentDrawable = null
        currentPath = null
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
}

class FreestyleDrawable(
    private val path: Path,
    override var color: Int,
    override var strokeWidth: Float,
    override var opacity: Int
) : DrawableItem {
    override val id: String = UUID.randomUUID().toString()
    override var isFilled: Boolean = false
    override var position: PointF = PointF(0f, 0f)
    override var rotation: Float = 0f

    private val paint = Paint().apply {
        this.color = color
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    fun addToPath(newPath: Path) {
        path.addPath(newPath)
    }
    
    override fun draw(canvas: Canvas, layerOpacity: Int) {
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.alpha = (opacity * layerOpacity / 255f).toInt()
        
        canvas.save()
        
        // Apply transformations
        canvas.translate(position.x, position.y)
        val bounds = getBounds()

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
        return FreestyleDrawable(
            Path(path),
            color,
            strokeWidth,
            opacity
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FreestyleDrawable) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}