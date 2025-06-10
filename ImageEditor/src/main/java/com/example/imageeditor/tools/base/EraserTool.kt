package com.example.imageeditor.tools.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import com.example.imageeditor.tools.base.BaseTool
import com.example.imageeditor.tools.base.DrawableItem
import java.util.UUID

class EraserTool : BaseTool() {
    private var path: Path? = null

    // Paint for the ERASE ACTION (used by EraserDrawable)
    private var erasePropertiesPaint: Paint = Paint().apply {
        color = Color.TRANSPARENT // Color itself doesn't matter much for CLEAR mode
        strokeWidth = 30f // Default, will be set by setStrokeWidth
        style = Paint.Style.STROKE // Required for path drawing
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) // This is for EraserDrawable
    }

    // Paint ONLY for the live preview
    private var previewPaint: Paint = Paint().apply {
        color = Color.argb(100, 128, 128, 128) // Semi-transparent gray
        strokeWidth = erasePropertiesPaint.strokeWidth // Keep in sync
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        // NO xfermode here, or default SRC_OVER for preview
    }

    private var currentX: Float = 0f
    private var currentY: Float = 0f
    private var startX: Float = 0f
    private var startY: Float = 0f

    override fun onTouchStart(event: MotionEvent) {
        path = Path()
        startX = event.x
        startY = event.y
        path?.moveTo(startX, startY)
        currentX = startX
        currentY = startY
    }

    override fun onTouchMove(event: MotionEvent) {
        currentX = event.x
        currentY = event.y
        path?.lineTo(currentX, currentY)
        startX = currentX
        startY = currentY
        // Ensure previewPaint's strokeWidth is updated if erasePropertiesPaint.strokeWidth changes dynamically elsewhere
        if (previewPaint.strokeWidth != erasePropertiesPaint.strokeWidth) {
            previewPaint.strokeWidth = erasePropertiesPaint.strokeWidth
        }
    }

    override fun onTouchEnd(event: MotionEvent) {
        // path is used by createDrawableItem, then reset
        // No need to nullify path here if createDrawableItem handles it
    }

    override fun drawPreview(canvas: Canvas) {
        path?.let { canvas.drawPath(it, previewPaint) } // Use previewPaint for drawing preview
    }

    override fun createDrawableItem(): DrawableItem? {
        if (path?.isEmpty == false) {
            val createdPath = Path(path) // Create a new Path instance for the drawable
            path = null // Reset path for the next touch gesture after creating the item
            return EraserDrawable(
                path = createdPath,
                strokeWidth = erasePropertiesPaint.strokeWidth, // Use from erasePropertiesPaint
                opacity = erasePropertiesPaint.alpha // Use from erasePropertiesPaint (typically 255)
            )
        }
        path = null // Also reset if path was empty
        return null
    }

    fun setStrokeWidth(width: Float) {
        erasePropertiesPaint.strokeWidth = width
        previewPaint.strokeWidth = width // Keep them in sync
    }
}

class EraserDrawable(
    private val path: Path,
    override var strokeWidth: Float,
    override var opacity: Int // Opacity here refers to the alpha of the erasePropertiesPaint
) : DrawableItem {
    override val id: String = UUID.randomUUID().toString()
    override var color: Int = Color.TRANSPARENT // EraserDrawable inherently "clears" color
    override var isFilled: Boolean = false // Eraser is never filled
    override var position: PointF = PointF(0f, 0f)
    override var rotation: Float = 0f

    private val eraserPaint = Paint().apply {
        this.color = Color.TRANSPARENT // Base color for CLEAR mode
        this.strokeWidth = this@EraserDrawable.strokeWidth
        this.style = Paint.Style.STROKE
        this.isAntiAlias = true
        this.strokeJoin = Paint.Join.ROUND
        this.strokeCap = Paint.Cap.ROUND
        this.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        this.alpha = this@EraserDrawable.opacity // Apply opacity, though CLEAR often ignores source alpha
    }

    override fun draw(canvas: Canvas, layerOpacity: Int) {
        // Update strokeWidth if it has been changed externally (e.g., by selection tool)
        if (eraserPaint.strokeWidth != this.strokeWidth) {
            eraserPaint.strokeWidth = this.strokeWidth
        }
        // Update opacity if changed
        if (eraserPaint.alpha != this.opacity) {
            eraserPaint.alpha = this.opacity
        }
        canvas.drawPath(path, eraserPaint)
    }

    override fun contains(x: Float, y: Float): Boolean {
        val bounds = getBounds()
        return bounds.contains(x, y)
    }

    override fun getBounds(): android.graphics.RectF {
        val bounds = android.graphics.RectF()
        path.computeBounds(bounds, true)
        return bounds
    }

    override fun transform(matrix: android.graphics.Matrix) {
        path.transform(matrix)
    }

    override fun copy(): DrawableItem {
        val newPath = Path(path)
        return EraserDrawable(newPath, strokeWidth, opacity)
    }
} 