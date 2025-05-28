package com.example.imageeditor.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.RequiresApi
import com.example.imageeditor.managers.HistoryManager
import com.example.imageeditor.managers.LayerBitmapState
import com.example.imageeditor.managers.LayerManager
import com.example.imageeditor.models.BrushStyle
import com.example.imageeditor.tools.DrawingTool
import com.example.imageeditor.tools.FreeBrushTool
import kotlin.math.max
import kotlin.math.min

class PaintCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var layerManager: LayerManager
    private lateinit var historyManager: HistoryManager
    private var currentBrushStyle: BrushStyle = BrushStyle()
    private var currentDrawingTool: DrawingTool = FreeBrushTool()
    private var isViewReady = false
    private var defaultBackgroundColor: Int = Color.WHITE // Store it here too

    // Zoom and Pan
    private var scaleFactor = 1.0f
    private var translationX = 0.0f
    private var translationY = 0.0f
    private var lastTouchX = 0.0f
    private var lastTouchY = 0.0f
    private val transformMatrix = Matrix()
    private val inverseTransformMatrix = Matrix()

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var mode = Mode.NONE

    private enum class Mode {
        NONE,
        DRAG, // Panning
        ZOOM
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScaleFactor = scaleFactor * detector.scaleFactor
            // Limit zoom levels
            scaleFactor = max(0.1f, min(newScaleFactor, 10.0f))
            
            // Focus zoom on the pinch center
            val focusX = detector.focusX
            val focusY = detector.focusY

            // Adjust translation to keep the focus point stationary during zoom
            // The formula is: T' = focus - (focus - T) * (S'/S)
            // where T is current translation, T' is new translation, S is current scale, S' is new scale
            translationX = focusX - (focusX - translationX) * (scaleFactor / (scaleFactor / detector.scaleFactor)) // Simplified: detector.scaleFactor gives S'/S
            translationY = focusY - (focusY - translationY) * (scaleFactor / (scaleFactor / detector.scaleFactor))
            
            updateTransformMatrix()
            invalidate()
            return true
        }
    }
    private val scaleDetector = ScaleGestureDetector(context, scaleListener)

    init {
        currentDrawingTool.setBrushStyle(currentBrushStyle)
    }

    private fun updateTransformMatrix() {
        transformMatrix.reset()
        transformMatrix.postTranslate(-width / 2f, -height / 2f) // Center before scaling
        transformMatrix.postScale(scaleFactor, scaleFactor)
        transformMatrix.postTranslate(width / 2f + translationX, height / 2f + translationY)
        
        // Update inverse matrix for transforming touch events
        transformMatrix.invert(inverseTransformMatrix)
    }
    
    private fun getTransformedMotionEvent(event: MotionEvent): MotionEvent {
        val transformedEvent = MotionEvent.obtain(event)
        val coords = floatArrayOf(event.x, event.y)
        inverseTransformMatrix.mapPoints(coords)
        transformedEvent.setLocation(coords[0], coords[1])
        return transformedEvent
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            if (!isViewReady) {
                layerManager = LayerManager(w, h)
                layerManager.setDefaultBackgroundColor(defaultBackgroundColor) // Pass it on init
                historyManager = HistoryManager()
            } else {
                layerManager.resize(w, h)
                historyManager.clearHistory()
            }
            isViewReady = true
            updateTransformMatrix() // Update matrix on size change
            currentDrawingTool.setBrushStyle(currentBrushStyle)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isViewReady) return

        canvas.save()
        canvas.concat(transformMatrix) // Apply zoom/pan transformations

        val compositeBitmap = layerManager.getCompositeBitmap()
        canvas.drawBitmap(compositeBitmap, 0f, 0f, null)
        compositeBitmap.recycle()

        currentDrawingTool.onDraw(canvas) // Tool previews are drawn in transformed space
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isViewReady) return false

        scaleDetector.onTouchEvent(event) // Pass to scale detector first

        val transformedEvent = getTransformedMotionEvent(event) // Transform event for tools

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
                mode = Mode.DRAG

                // Pass transformed event to tool for drawing logic
                layerManager.currentLayer?.let {
                    if(!scaleDetector.isInProgress) historyManager.saveState(it.id, it.bitmap)
                }
                currentDrawingTool.onTouch(transformedEvent, this)
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress) {
                    mode = Mode.ZOOM
                } else if (mode == Mode.DRAG && event.getPointerId(0) == activePointerId) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    translationX += dx
                    translationY += dy
                    updateTransformMatrix()
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
                // Pass transformed event to tool only if not zooming/panning by this view
                if (mode != Mode.ZOOM ) { // Allow tool interaction during pan if desired, but for drawing usually not.
                    // If we are panning the canvas, we might not want the tool to also process moves.
                    // However, for drawing while panning (less common), this would be needed.
                    // For now, let tool handle move only if not in ZOOM mode.
                    // It's complex. If dragging the canvas, the tool's interpretation of move is on a moving surface.
                    // Simplest for now: if we are zooming or panning canvas, tool does not get move event.
                    // The tool will receive the ACTION_UP at the transformed location.
                    if (mode != Mode.DRAG) currentDrawingTool.onTouch(transformedEvent, this)
                } else {
                     // If zooming, reset the current tool to avoid partial draws
                     currentDrawingTool.reset()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                mode = Mode.NONE
                // Pass transformed event to tool
                currentDrawingTool.onTouch(transformedEvent, this)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // If a second pointer goes down and scale isn't in progress, it might be start of zoom
                // or just another pointer. ScaleDetector will handle it.
                 currentDrawingTool.reset() // Reset tool on multi-touch start to prevent drawing issues
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // If the up pointer was the active one for dragging, pick a new active pointer
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) { // Check if new pointer exists
                         lastTouchX = event.getX(newPointerIndex)
                         lastTouchY = event.getY(newPointerIndex)
                         activePointerId = event.getPointerId(newPointerIndex)
                         mode = Mode.DRAG // Resume dragging with the remaining pointer if any
                    } else {
                        mode = Mode.NONE
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                } else {
                     // If a non-active pointer went up, it doesn't change current drag/zoom mode determined by ScaleDetector
                }
                // Pass transformed event to tool. This could be an ACTION_UP for a path.
                currentDrawingTool.onTouch(transformedEvent, this)
            }
        }
        transformedEvent.recycle() // Recycle the cloned event
        return true // Consume all touch events if view is ready
    }

    fun addPath(path: Path, style: BrushStyle, isEraser: Boolean = false) {
        if (!isViewReady) return
        layerManager.currentLayer?.let { activeLayer ->
            val layerCanvas = Canvas(activeLayer.bitmap)
            val paintToUse = Paint().apply {
                isAntiAlias = true
                isDither = true
                this.style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = style.color
                // Stroke width should be scaled inversely to the canvas scale for consistent appearance
                strokeWidth = style.strokeWidth / scaleFactor 
                if (isEraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            }
            layerCanvas.drawPath(path, paintToUse)
            invalidate()
        }
    }

    // Reset zoom and pan to default
    fun resetZoomPan() {
        scaleFactor = 1.0f
        translationX = 0.0f
        translationY = 0.0f
        updateTransformMatrix()
        invalidate()
    }
    
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun undo(): Boolean {
        if (!isViewReady || !historyManager.canUndo()) return false
        layerManager.currentLayer?.let { activeLayer ->
            val currentState = LayerBitmapState(activeLayer.id, activeLayer.bitmap.copy(activeLayer.bitmap.config!!, true))
            historyManager.undo(currentState)?.let { stateToRestore ->
                if (activeLayer.id == stateToRestore.layerId) {
                    layerManager.updateLayerBitmap(stateToRestore.layerId, stateToRestore.bitmap)
                    stateToRestore.bitmap.recycle()
                    invalidate()
                    return true
                } else {
                    currentState.bitmap.recycle()
                    return false 
                }
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun redo(): Boolean {
        if (!isViewReady || !historyManager.canRedo()) return false
        layerManager.currentLayer?.let { activeLayer ->
             val currentState = LayerBitmapState(activeLayer.id, activeLayer.bitmap.copy(activeLayer.bitmap.config!!, true))
            historyManager.redo(currentState)?.let { stateToRestore ->
                if (activeLayer.id == stateToRestore.layerId) {
                    layerManager.updateLayerBitmap(stateToRestore.layerId, stateToRestore.bitmap)
                    stateToRestore.bitmap.recycle()
                    invalidate()
                    return true
                } else {
                    currentState.bitmap.recycle()
                    return false
                }
            }
        }
        return false
    }

    fun clearCurrentLayer() {
        if (!isViewReady) return
        layerManager.currentLayer?.let { activeLayer ->
            historyManager.saveState(activeLayer.id, activeLayer.bitmap)
            val canvas = Canvas(activeLayer.bitmap)
            val clearColor = if (layerManager.allLayers.firstOrNull() == activeLayer && activeLayer.name == "Background") {
                Color.WHITE
            } else {
                Color.TRANSPARENT
            }
            if (clearColor == Color.WHITE) {
                canvas.drawColor(Color.WHITE)
            } else {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            currentDrawingTool.reset()
            invalidate()
        }
    }

    fun resetAndClearHistory() {
        if(::historyManager.isInitialized) historyManager.clearHistory()
        invalidate()
    }

    fun setCurrentTool(tool: DrawingTool) {
        currentDrawingTool.reset()
        currentDrawingTool = tool
        currentDrawingTool.setBrushStyle(currentBrushStyle)
        // Tool needs to be aware of current canvas transform for its internal logic if any
        // This is tricky. For now, tool receives transformed coordinates.
        invalidate()
    }

    fun getCurrentBrushStyle(): BrushStyle {
        return currentBrushStyle
    }

    fun setCurrentBrushStyle(style: BrushStyle) {
        currentBrushStyle = style
        currentDrawingTool.setBrushStyle(currentBrushStyle)
        // Brush stroke width might need to appear consistent regardless of zoom.
        // This is handled in addPath for drawing, but preview by tool might need adjustment.
    }

    fun setBrushColor(color: Int) {
        currentBrushStyle = currentBrushStyle.copy(color = color)
        currentDrawingTool.setBrushStyle(currentBrushStyle)
    }

    fun setStrokeWidth(width: Float) {
        // Store the desired stroke width at 1x zoom
        currentBrushStyle = currentBrushStyle.copy(strokeWidth = width)
        currentDrawingTool.setBrushStyle(currentBrushStyle) 
        // Actual drawn width will be adjusted in addPath and tool's onDraw based on scaleFactor
    }

    fun getCurrentLayerBitmap(): Bitmap? {
        if (!isViewReady) return null
        return layerManager.currentLayer?.bitmap
    }

    fun loadBitmapIntoCurrentLayer(bitmap: Bitmap) {
        if (!isViewReady) return
        layerManager.currentLayer?.let { activeLayer ->
            historyManager.saveState(activeLayer.id, activeLayer.bitmap)
            val canvas = Canvas(activeLayer.bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(bitmap, 0f, 0f, null) 
            invalidate()
        }
    }
    
    fun getLayerManager(): LayerManager? {
        return if (::layerManager.isInitialized) layerManager else null
    }

    fun getBitmap(): Bitmap? {
        if (!isViewReady) return null
        // This should return the untransformed, full composite bitmap.
        // For export, we don't want the current view's zoom/pan applied to the output file.
        // LayerManager.getCompositeBitmap() already provides this.
        val composite = layerManager.getCompositeBitmap()
        return composite
    }

    fun setDefaultBackgroundColor(color: Int) {
        defaultBackgroundColor = color
        if (::layerManager.isInitialized) {
            layerManager.setDefaultBackgroundColor(color)
        }
        invalidate() // In case background color change needs redraw
    }
}
