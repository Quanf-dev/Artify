package com.example.imageeditor.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.imageeditor.domain.LayerManager
import com.example.imageeditor.tools.base.BaseTool
import com.example.imageeditor.tools.base.SelectionTool
import com.example.imageeditor.tools.base.DrawableItem
import com.example.imageeditor.tools.base.BlurTool
import com.example.imageeditor.tools.shapes.RectangleTool
import com.example.imageeditor.tools.shapes.CircleTool
import com.example.imageeditor.tools.shapes.ArrowTool
import com.example.imageeditor.tools.shapes.DashLineTool
import com.example.imageeditor.tools.shapes.LineTool
import com.example.imageeditor.tools.freestyle.FreestyleTool
import com.example.imageeditor.utils.HistoryManager
import kotlin.math.max
import kotlin.math.min

class PaintEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Managers
    private val layerManager = LayerManager()
    private val historyManager = HistoryManager()
    
    // Drawing properties
    var currentTool: BaseTool = SelectionTool(layerManager, this)
        private set
    private var currentColor: Int = Color.BLACK
    private var currentStrokeWidth: Float = 5f
    private var currentOpacity: Int = 255
    private var currentFillMode: Boolean = false
    
    // Transformation properties
    private val viewMatrix = Matrix()
    private var currentScale = 1f
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // Background image properties
    private var backgroundImage: Bitmap? = null
    private val backgroundSrcRect = Rect()
    private val backgroundDstRectF = RectF()
    
    // Bitmap for caching drawings
    private var drawingCacheBitmap: Bitmap? = null
    private var drawingCacheCanvas: Canvas? = null

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        if (w > 0 && h > 0) {
            drawingCacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawingCacheCanvas = Canvas(drawingCacheBitmap!!)
            updateBackgroundRects(w, h)
            redrawAllLayers()
        }
    }
    
    private fun updateBackgroundRects(viewWidth: Int, viewHeight: Int) {
        backgroundImage?.let { bitmap ->
            val imageRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
            
            if (imageRatio > viewRatio) {
                // Image is wider than view
                val scaledHeight = (viewWidth / imageRatio).toInt()
                val y = (viewHeight - scaledHeight) / 2
                backgroundDstRectF.set(0f, y.toFloat(), viewWidth.toFloat(), (y + scaledHeight).toFloat())
            } else {
                // Image is taller than view
                val scaledWidth = (viewHeight * imageRatio).toInt()
                val x = (viewWidth - scaledWidth) / 2
                backgroundDstRectF.set(x.toFloat(), 0f, (x + scaledWidth).toFloat(), viewHeight.toFloat())
            }
            
            backgroundSrcRect.set(0, 0, bitmap.width, bitmap.height)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(viewMatrix)

        // Luôn fill trắng trước
        canvas.drawColor(Color.WHITE)

        // Sau đó mới vẽ background (nếu có)
        backgroundImage?.let {
            canvas.drawBitmap(it, backgroundSrcRect, backgroundDstRectF, null)
        }

        drawingCacheBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        currentTool.drawPreview(canvas)
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        
        val transformedEvent = MotionEvent.obtain(event)
        val inverseMatrix = Matrix()
        viewMatrix.invert(inverseMatrix)
        val points = floatArrayOf(event.x, event.y)
        inverseMatrix.mapPoints(points)
        transformedEvent.setLocation(points[0], points[1])
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1 && !scaleGestureDetector.isInProgress) {
                    currentTool.onTouchStart(transformedEvent)
                    invalidate()
                } 
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleGestureDetector.isInProgress) {
                } else if (event.pointerCount > 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    viewMatrix.postTranslate(dx, dy)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                } else {
                    currentTool.onTouchMove(transformedEvent)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!scaleGestureDetector.isInProgress) {
                    val drawableItem = currentTool.createDrawableItem()
                    currentTool.onTouchEnd(transformedEvent)
                    
                    if (drawableItem != null) {
                        historyManager.addHistoryItem(drawableItem)
                        layerManager.getActiveLayer()?.addDrawable(drawableItem)
                        redrawAllLayers()
                        invalidate()
                    }
                }
                return true
            }
        }
        transformedEvent.recycle()
        return super.onTouchEvent(event)
    }
    
    fun redrawAllLayers() {
        drawingCacheCanvas?.let { canvas ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            layerManager.draw(canvas)
        }
        invalidate()
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentScale *= detector.scaleFactor
            currentScale = max(0.1f, min(currentScale, 10.0f))
            
            viewMatrix.postScale(
                detector.scaleFactor,
                detector.scaleFactor,
                detector.focusX,
                detector.focusY
            )
            invalidate()
            return true
        }
    }
    
    fun setTool(tool: BaseTool) {
        currentTool.onToolDeselected()
        currentTool = tool
        
        // Apply current properties to the new tool
        when (tool) {
            is RectangleTool -> {
                tool.setColor(currentColor)
                tool.setStrokeWidth(currentStrokeWidth)
                tool.setOpacity(currentOpacity)
                tool.setFillMode(currentFillMode)
            }
            is CircleTool -> {
                tool.setColor(currentColor)
                tool.setStrokeWidth(currentStrokeWidth)
                tool.setOpacity(currentOpacity)
                tool.setFillMode(currentFillMode)
            }
            is ArrowTool -> {
                tool.setColor(currentColor)
                tool.setStrokeWidth(currentStrokeWidth)
                tool.setOpacity(currentOpacity)
            }
            is DashLineTool -> {
                tool.setColor(currentColor)
                tool.setStrokeWidth(currentStrokeWidth)
                tool.setOpacity(currentOpacity)
            }
            is LineTool -> {
                tool.setColor(currentColor)
                tool.setStrokeWidth(currentStrokeWidth)
                tool.setOpacity(currentOpacity)
            }
            is FreestyleTool -> {
                tool.setColor(currentColor)
                tool.setStrokeWidth(currentStrokeWidth)
                tool.setOpacity(currentOpacity)
            }
            is BlurTool -> {
                tool.setStrokeWidth(currentStrokeWidth)
            }
        }
        
        tool.onToolSelected()
        invalidate()
    }
    
    fun setColor(color: Int) {
        currentColor = color
        // Apply to current tool immediately
        when (currentTool) {
            is RectangleTool -> (currentTool as RectangleTool).setColor(color)
            is CircleTool -> (currentTool as CircleTool).setColor(color)
            is ArrowTool -> (currentTool as ArrowTool).setColor(color)
            is DashLineTool -> (currentTool as DashLineTool).setColor(color)
            is FreestyleTool -> (currentTool as FreestyleTool).setColor(color)
            is LineTool -> (currentTool as LineTool).setColor(color)
        }
        // Also update selected item if any
        currentTool.getSelectedItem()?.let {
            it.color = color
            redrawAllLayers()
            invalidate()
        }
    }
    
    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
        // Apply to current tool immediately
        when (currentTool) {
            is RectangleTool -> (currentTool as RectangleTool).setStrokeWidth(width)
            is CircleTool -> (currentTool as CircleTool).setStrokeWidth(width)
            is ArrowTool -> (currentTool as ArrowTool).setStrokeWidth(width)
            is DashLineTool -> (currentTool as DashLineTool).setStrokeWidth(width)
            is BlurTool -> (currentTool as BlurTool).setStrokeWidth(width)
            is FreestyleTool -> (currentTool as FreestyleTool).setStrokeWidth(width)
            is LineTool -> (currentTool as LineTool).setStrokeWidth(width)
        }
        // Also update selected item if any
        currentTool.getSelectedItem()?.let {
            it.strokeWidth = width
            redrawAllLayers()
            invalidate()
        }
    }
    
    fun setOpacity(opacity: Int) {
        currentOpacity = opacity
        // Apply to current tool immediately
        when (currentTool) {
            is RectangleTool -> (currentTool as RectangleTool).setOpacity(opacity)
            is CircleTool -> (currentTool as CircleTool).setOpacity(opacity)
            is ArrowTool -> (currentTool as ArrowTool).setOpacity(opacity)
            is DashLineTool -> (currentTool as DashLineTool).setOpacity(opacity)
            is FreestyleTool -> (currentTool as FreestyleTool).setOpacity(opacity)
            is LineTool -> (currentTool as LineTool).setOpacity(opacity)
        }
        // Also update selected item if any
        currentTool.getSelectedItem()?.let {
            it.opacity = opacity
            redrawAllLayers()
            invalidate()
        }
    }
    
    fun setFillMode(isFilled: Boolean) {
        currentFillMode = isFilled
        // Apply to current tool immediately
        when (currentTool) {
            is RectangleTool -> (currentTool as RectangleTool).setFillMode(isFilled)
            is CircleTool -> (currentTool as CircleTool).setFillMode(isFilled)
        }
        // Also update selected item if any
        currentTool.getSelectedItem()?.let {
            it.isFilled = isFilled
            redrawAllLayers()
            invalidate()
        }
    }
    
    fun setBackgroundImage(bitmap: Bitmap?) {
        backgroundImage = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
        backgroundImage?.let {
            updateBackgroundRects(width, height)
        }
        redrawAllLayers()
        invalidate()
    }
    
    fun undo() {
        val undoneItem = historyManager.undo()
        if (undoneItem != null) {
            layerManager.getActiveLayer()?.removeDrawable(undoneItem)
            redrawAllLayers()
            invalidate()
        }
    }
    
    fun redo() {
        val redoneItem = historyManager.redo()
        if (redoneItem != null) {
            layerManager.getActiveLayer()?.addDrawable(redoneItem)
            redrawAllLayers()
            invalidate()
        }
    }
    
    fun getBitmap(): Bitmap? {
        if (width <= 0 || height <= 0) return null
        
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        if (backgroundImage == null) {
            canvas.drawColor(Color.WHITE)
        }
        backgroundImage?.let {
            canvas.drawBitmap(it, backgroundSrcRect, backgroundDstRectF, null)
        }
        drawingCacheBitmap?.let{
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        return resultBitmap
    }
    
    fun resetViewTransformations() {
        viewMatrix.reset()
        currentScale = 1f
        invalidate()
    }
    
    fun clear() {
        historyManager.clear()
        layerManager.clear()
        redrawAllLayers()
        invalidate()
    }

    fun getLayerManager(): LayerManager = layerManager

    fun getSelectedItem(): DrawableItem? {
        return (currentTool as? SelectionTool)?.getSelectedItem()
    }

    private fun saveDrawing() {
        // Implementation of saveDrawing method
    }
}