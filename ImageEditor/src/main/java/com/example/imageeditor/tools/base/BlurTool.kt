package com.example.imageeditor.tools.base

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.BlurMaskFilter
import android.view.MotionEvent
import com.example.imageeditor.tools.base.BaseTool
import com.example.imageeditor.tools.base.DrawableItem
import java.util.UUID

class BlurTool : BaseTool() {
    private var currentPath: Path? = null
    private var currentDrawable: BlurDrawable? = null
    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 50f
        style = Paint.Style.STROKE
        isAntiAlias = true
        maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
    }
    
    override fun onTouchStart(event: MotionEvent) {
        currentPath = Path()
        currentPath?.moveTo(event.x, event.y)
        currentDrawable = null
    }
    
    override fun onTouchMove(event: MotionEvent) {
        currentPath?.lineTo(event.x, event.y)
    }
    
    override fun onTouchEnd(event: MotionEvent) {
        currentPath = null
    }
    
    override fun drawPreview(canvas: Canvas) {
        currentPath?.let { canvas.drawPath(it, paint) }
    }
    
    override fun createDrawableItem(): DrawableItem? {
        if (currentPath == null || currentPath!!.isEmpty) {
            return null
        }
        val pathToSave = Path(currentPath)
        currentDrawable = BlurDrawable(
            path = pathToSave,
            strokeWidth = paint.strokeWidth,
            blurRadius = 25f
        )
        return currentDrawable
    }
    
    override fun getSelectedItem(): DrawableItem? = currentDrawable
    
    override fun onToolDeselected() {
        currentDrawable = null
    }
    
    fun setStrokeWidth(width: Float) {
        paint.strokeWidth = width
        currentDrawable?.strokeWidth = width
    }
    
    fun setBlurRadius(radius: Float) {
        paint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        currentDrawable?.setBlurRadius(radius)
    }
}

class BlurDrawable(
    private val path: Path,
    override var strokeWidth: Float,
    private var blurRadius: Float
) : DrawableItem {
    override val id: String = UUID.randomUUID().toString()
    override var color: Int = Color.TRANSPARENT
    override var opacity: Int = 255
    override var isFilled: Boolean = false
    override var position: PointF = PointF(0f, 0f)
    override var rotation: Float = 0f

    private val paint = Paint().apply {
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
        isAntiAlias = true
        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
    }
    
    override fun draw(canvas: Canvas, layerOpacity: Int) {
        // Create a temporary bitmap for the blur effect
        val bounds = getBounds()
        val tempBitmap = Bitmap.createBitmap(
            bounds.width().toInt() + 100, // Add padding for blur overflow
            bounds.height().toInt() + 100,
            Bitmap.Config.ARGB_8888
        )
        val tempCanvas = Canvas(tempBitmap)
        
        // Offset the path to account for the padding
        val offsetPath = Path(path)
        offsetPath.offset(50f - bounds.left, 50f - bounds.top)
        
        // Draw the path with blur
        paint.strokeWidth = strokeWidth
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        tempCanvas.drawPath(offsetPath, paint)
        
        // Draw the blurred bitmap back to the main canvas
        canvas.save()
        
        // Apply transformations
        canvas.translate(position.x, position.y)

        // Draw the blurred bitmap
        canvas.drawBitmap(
            tempBitmap,
            bounds.left - 50f,
            bounds.top - 50f,
            null
        )
        
        canvas.restore()
        
        // Clean up
        tempBitmap.recycle()
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
    
    override fun transform(matrix: android.graphics.Matrix) {
        path.transform(matrix)
    }
    
    override fun copy(): DrawableItem {
        return BlurDrawable(
            Path(path),
            strokeWidth,
            blurRadius
        )
    }
    
    fun setBlurRadius(radius: Float) {
        blurRadius = radius
        paint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
    }
} 