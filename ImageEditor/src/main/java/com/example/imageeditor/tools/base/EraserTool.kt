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
    private var paint: Paint = Paint().apply {
        color = Color.TRANSPARENT
        strokeWidth = 20f
        style = Paint.Style.STROKE
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
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
        path?.quadTo(
            startX,
            startY,
            (currentX + startX) / 2,
            (currentY + startY) / 2
        )
        startX = currentX
        startY = currentY
    }

    override fun onTouchEnd(event: MotionEvent) {
        path = null
    }

    override fun drawPreview(canvas: Canvas) {
        path?.let { canvas.drawPath(it, paint) }
    }

    override fun createDrawableItem(): DrawableItem? {
        if (path?.isEmpty == false) {
            return EraserDrawable(
                path = Path(path),
                strokeWidth = paint.strokeWidth,
                opacity = paint.alpha
            )
        }
        return null
    }
}

class EraserDrawable(
    private val path: Path,
    override var strokeWidth: Float,
    override var opacity: Int
) : DrawableItem {
    override val id: String = UUID.randomUUID().toString()
    override var color: Int = Color.TRANSPARENT
    override var isFilled: Boolean = false
    override var position: PointF = PointF(0f, 0f)
    override var rotation: Float = 0f
    private val eraserPaint = Paint().apply {
        color = Color.TRANSPARENT
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        alpha = opacity
    }

    override fun draw(canvas: Canvas, layerOpacity: Int) {
        eraserPaint.alpha = this.opacity
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