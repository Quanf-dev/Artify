package com.example.imageeditor.tools.text

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import com.example.imageeditor.tools.base.BaseTool
import com.example.imageeditor.tools.base.DrawableItem
import java.util.UUID

class TextTool : BaseTool() {
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var isDrawing: Boolean = false
    private var currentDrawable: TextDrawable? = null
    
    private val paint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
        isAntiAlias = true
    }
    
    private var text: String = "Nhập văn bản"
    private var textAlign: Paint.Align = Paint.Align.CENTER
    private var typeface: Typeface = Typeface.DEFAULT
    private var textBackgroundColor: Int = Color.TRANSPARENT
    private var textBackgroundAlpha: Int = 0 // 0-255, 0 là trong suốt
    
    override fun onTouchStart(event: MotionEvent) {
        startX = event.x
        startY = event.y
        isDrawing = true
        
        // Tạo TextDrawable mới với thuộc tính hiện tại
        currentDrawable = TextDrawable(
            text = text,
            color = paint.color,
            textSize = paint.textSize,
            opacity = paint.alpha,
            align = textAlign,
            typeface = typeface,
            backgroundColor = textBackgroundColor,
            backgroundAlpha = textBackgroundAlpha
        )
        
        // Đặt vị trí ban đầu ở giữa màn hình thay vì tại điểm chạm
        val view = event.source as? android.view.View
        if (view != null) {
            // Lấy kích thước màn hình từ view
            val centerX = view.width / 2f
            val centerY = view.height / 2f
            currentDrawable?.position = PointF(centerX, centerY)
        } else {
            // Fallback nếu không lấy được view
            currentDrawable?.position = PointF(startX, startY)
        }
    }
    
    override fun onTouchMove(event: MotionEvent) {
        // Không cần xử lý di chuyển khi thêm text
    }
    
    override fun onTouchEnd(event: MotionEvent) {
        if (!isDrawing) return
        isDrawing = false
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
    
    fun setText(text: String) {
        this.text = text
        currentDrawable?.text = text
    }
    
    fun setColor(color: Int) {
        paint.color = color
        currentDrawable?.color = color
    }
    
    fun setTextSize(size: Float) {
        paint.textSize = size
        currentDrawable?.textSize = size
    }
    
    fun setOpacity(opacity: Int) {
        paint.alpha = opacity
        currentDrawable?.opacity = opacity
    }
    
    fun setTextAlign(align: Paint.Align) {
        textAlign = align
        currentDrawable?.align = align
    }
    
    fun setTypeface(typeface: Typeface) {
        this.typeface = typeface
        currentDrawable?.typeface = typeface
    }
    
    fun setBackgroundColor(color: Int) {
        textBackgroundColor = color
        currentDrawable?.backgroundColor = color
    }
    
    fun setBackgroundAlpha(alpha: Int) {
        textBackgroundAlpha = alpha
        currentDrawable?.backgroundAlpha = alpha
    }
}

class TextDrawable(
    var text: String,
    override var color: Int,
    var textSize: Float,
    override var opacity: Int,
    var align: Paint.Align,
    var typeface: Typeface,
    var backgroundColor: Int,
    var backgroundAlpha: Int
) : DrawableItem {
    override val id: String = UUID.randomUUID().toString()
    override var isFilled: Boolean = false
    override var position: PointF = PointF(0f, 0f)
    override var rotation: Float = 0f
    override var strokeWidth: Float = 1f
    
    private val textPaint = Paint().apply {
        this.color = color
        this.textSize = textSize
        this.isAntiAlias = true
        this.textAlign = align
        this.typeface = typeface
    }
    
    private val backgroundPaint = Paint().apply {
        this.color = backgroundColor
        this.alpha = backgroundAlpha
        this.style = Paint.Style.FILL
    }
    
    private val controlPaint = Paint().apply {
        this.color = Color.BLUE
        this.style = Paint.Style.STROKE
        this.strokeWidth = 2f
    }
    
    private val deletePaint = Paint().apply {
        this.color = Color.RED
        this.style = Paint.Style.FILL_AND_STROKE
    }
    
    private val editPaint = Paint().apply {
        this.color = Color.GREEN
        this.style = Paint.Style.FILL_AND_STROKE
    }
    
    private val rotatePaint = Paint().apply {
        this.color = Color.BLUE
        this.style = Paint.Style.FILL_AND_STROKE
    }
    
    private val controlSize = 30f
    private val controlPadding = 10f
    
    override fun draw(canvas: Canvas, layerOpacity: Int) {
        textPaint.color = color
        textPaint.textSize = textSize
        textPaint.alpha = (opacity * layerOpacity / 255f).toInt()
        textPaint.textAlign = align
        textPaint.typeface = typeface
        
        backgroundPaint.color = backgroundColor
        backgroundPaint.alpha = (backgroundAlpha * layerOpacity / 255f).toInt()
        
        canvas.save()
        
        // Áp dụng biến đổi
        canvas.translate(position.x, position.y)
        
        val bounds = getTextBounds()
        val padding = 20f
        
        // Vẽ background nếu cần
        if (backgroundAlpha > 0) {
            val bgRect = RectF(
                bounds.left - padding,
                bounds.top - padding,
                bounds.right + padding,
                bounds.bottom + padding
            )
            canvas.drawRect(bgRect, backgroundPaint)
        }
        
        // Vẽ text
        canvas.drawText(text, 0f, 0f, textPaint)
        
        canvas.restore()
    }
    
    fun drawWithControls(canvas: Canvas, layerOpacity: Int) {
        // Vẽ text bình thường
        draw(canvas, layerOpacity)
        
        canvas.save()
        
        // Áp dụng biến đổi
        canvas.translate(position.x, position.y)
        
        val bounds = getTextBounds()
        val padding = 20f
        
        // Vẽ khung chọn
        val selectionRect = RectF(
            bounds.left - padding,
            bounds.top - padding,
            bounds.right + padding,
            bounds.bottom + padding
        )
        canvas.drawRect(selectionRect, controlPaint)
        
        // Vẽ nút xóa (góc trên bên phải)
        val deleteRect = RectF(
            selectionRect.right - controlSize / 2,
            selectionRect.top - controlSize / 2,
            selectionRect.right + controlSize / 2,
            selectionRect.top + controlSize / 2
        )
        canvas.drawCircle(deleteRect.centerX(), deleteRect.centerY(), controlSize / 2, deletePaint)
        
        // Vẽ nút chỉnh sửa (góc trên bên trái)
        val editRect = RectF(
            selectionRect.left - controlSize / 2,
            selectionRect.top - controlSize / 2,
            selectionRect.left + controlSize / 2,
            selectionRect.top + controlSize / 2
        )
        canvas.drawCircle(editRect.centerX(), editRect.centerY(), controlSize / 2, editPaint)
        
        // Vẽ nút xoay (góc dưới bên phải)
        val rotateRect = RectF(
            selectionRect.right - controlSize / 2,
            selectionRect.bottom - controlSize / 2,
            selectionRect.right + controlSize / 2,
            selectionRect.bottom + controlSize / 2
        )
        canvas.drawCircle(rotateRect.centerX(), rotateRect.centerY(), controlSize / 2, rotatePaint)
        
        canvas.restore()
    }
    
    override fun contains(x: Float, y: Float): Boolean {
        val bounds = getBounds()
        return bounds.contains(x, y)
    }
    
    fun containsDeleteControl(x: Float, y: Float): Boolean {
        val bounds = getTextBounds()
        val padding = 20f
        
        val selectionRect = RectF(
            bounds.left - padding,
            bounds.top - padding,
            bounds.right + padding,
            bounds.bottom + padding
        )
        
        val deleteRect = RectF(
            position.x + selectionRect.right - controlSize / 2,
            position.y + selectionRect.top - controlSize / 2,
            position.x + selectionRect.right + controlSize / 2,
            position.y + selectionRect.top + controlSize / 2
        )
        
        return deleteRect.contains(x, y)
    }
    
    fun containsEditControl(x: Float, y: Float): Boolean {
        val bounds = getTextBounds()
        val padding = 20f
        
        val selectionRect = RectF(
            bounds.left - padding,
            bounds.top - padding,
            bounds.right + padding,
            bounds.bottom + padding
        )
        
        val editRect = RectF(
            position.x + selectionRect.left - controlSize / 2,
            position.y + selectionRect.top - controlSize / 2,
            position.x + selectionRect.left + controlSize / 2,
            position.y + selectionRect.top + controlSize / 2
        )
        
        return editRect.contains(x, y)
    }
    
    fun containsRotateControl(x: Float, y: Float): Boolean {
        val bounds = getTextBounds()
        val padding = 20f
        
        val selectionRect = RectF(
            bounds.left - padding,
            bounds.top - padding,
            bounds.right + padding,
            bounds.bottom + padding
        )
        
        val rotateRect = RectF(
            position.x + selectionRect.right - controlSize / 2,
            position.y + selectionRect.bottom - controlSize / 2,
            position.x + selectionRect.right + controlSize / 2,
            position.y + selectionRect.bottom + controlSize / 2
        )
        
        return rotateRect.contains(x, y)
    }
    
    override fun getBounds(): RectF {
        val bounds = getTextBounds()
        val padding = 20f
        
        val result = RectF(
            position.x + bounds.left - padding,
            position.y + bounds.top - padding,
            position.x + bounds.right + padding,
            position.y + bounds.bottom + padding
        )
        
        return result
    }
    
    private fun getTextBounds(): RectF {
        val bounds = RectF()
        val textWidth = textPaint.measureText(text)
        val textHeight = textPaint.descent() - textPaint.ascent()
        
        when (align) {
            Paint.Align.LEFT -> {
                bounds.left = 0f
                bounds.right = textWidth
            }
            Paint.Align.CENTER -> {
                bounds.left = -textWidth / 2
                bounds.right = textWidth / 2
            }
            Paint.Align.RIGHT -> {
                bounds.left = -textWidth
                bounds.right = 0f
            }
        }
        
        bounds.top = textPaint.ascent()
        bounds.bottom = textPaint.descent()
        
        return bounds
    }
    
    override fun transform(matrix: Matrix) {
        val values = FloatArray(9)
        matrix.getValues(values)
        
        // Áp dụng biến đổi cho vị trí
        val px = position.x
        val py = position.y
        position.x = values[Matrix.MSCALE_X] * px + values[Matrix.MSKEW_X] * py + values[Matrix.MTRANS_X]
        position.y = values[Matrix.MSKEW_Y] * px + values[Matrix.MSCALE_Y] * py + values[Matrix.MTRANS_Y]
    }
    
    override fun copy(): DrawableItem {
        return TextDrawable(
            text,
            color,
            textSize,
            opacity,
            align,
            typeface,
            backgroundColor,
            backgroundAlpha
        )
    }
}