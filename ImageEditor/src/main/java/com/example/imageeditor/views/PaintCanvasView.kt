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
import com.example.imageeditor.tools.ShapeTool
import com.example.imageeditor.tools.EraserTool
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

    private fun clampTranslations() {
        val scaledWidth = width * scaleFactor
        val scaledHeight = height * scaleFactor

        // Clamp X
        if (scaledWidth > width) {
            val maxTx = (width / 2f) * (scaleFactor - 1)
            val minTx = (width / 2f) * (1 - scaleFactor)
            translationX = Math.max(minTx, Math.min(translationX, maxTx))
        } else {
            // val limitX = (width - scaledWidth) / 2f // This was for a different centering logic
            if (scaledWidth < width) {
                 // Allow to pan until its edge reaches the view edge, or a bit more to ensure it can be moved fully
                translationX = Math.max(translationX, -(scaledWidth - width / 2f + (width - scaledWidth)/2f )) // Corrected limit logic
                translationX = Math.min(translationX, width / 2f - (width-scaledWidth)/2f) // Corrected limit logic
            } else { // If image is same width as view, or content is wider but scaled smaller than view
                translationX = 0f
            }
        }

        // Clamp Y
        if (scaledHeight > height) {
            val maxTy = (height / 2f) * (scaleFactor - 1)
            val minTy = (height / 2f) * (1 - scaleFactor)
            translationY = Math.max(minTy, Math.min(translationY, maxTy))
        } else {
            // val limitY = (height - scaledHeight) / 2f
            if (scaledHeight < height) {
                translationY = Math.max(translationY, -(scaledHeight - height/2f + (height - scaledHeight)/2f))
                translationY = Math.min(translationY, height/2f - (height - scaledHeight)/2f)
            } else {
                translationY = 0f
            }
        }
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

        scaleDetector.onTouchEvent(event)
        val transformedEvent = getTransformedMotionEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
                mode = Mode.DRAG // Default mode for a single touch down

                layerManager.currentLayer?.let {
                    if (!scaleDetector.isInProgress) historyManager.saveState(it.id, it.bitmap)
                }
                currentDrawingTool.onTouch(transformedEvent, this)
                // canvasView.invalidate() in tool's onTouch if needed
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress) { // Zooming
                    mode = Mode.ZOOM
                    // Zoom transformations (scaleFactor, translationX/Y adjustments) are handled by
                    // scaleListener.onScale, which calls updateTransformMatrix() and invalidate().
                    currentDrawingTool.reset() // Reset tool as canvas is transforming
                    // invalidate() is called by onScale
                } else if (mode == Mode.DRAG && event.getPointerId(0) == activePointerId) { // Non-zooming drag with the primary pointer
                    
                    // Let the drawing tool process the move first
                    currentDrawingTool.onTouch(transformedEvent, this)

                    // Pan the canvas itself ONLY if a drawing-type tool is NOT active.
                    // Drawing tools (FreeBrush, Shape, Eraser) handle their own path creation based on the gesture.
                    val isDrawingToolCurrentlyActive = currentDrawingTool is FreeBrushTool ||
                                                     currentDrawingTool is ShapeTool ||
                                                     currentDrawingTool is EraserTool

                    if (!isDrawingToolCurrentlyActive) {
                        // This block is for panning the canvas itself if a non-drawing tool is active (e.g. a future PanTool)
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        translationX += dx // Tentatively update
                        translationY += dy // Tentatively update
                        clampTranslations()    // Clamp the member variables translationX, translationY
                        updateTransformMatrix()
                        lastTouchX = event.x
                        lastTouchY = event.y
                        invalidate() // Panning the canvas requires redraw
                    } else {
                        // If a drawing tool IS active, it should have called invalidate() in its onTouch if it updated the path.
                        // If not, an invalidate here ensures the view redraws the tool's preview.
                         invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentDrawingTool.onTouch(transformedEvent, this) // Let tool finalize its action

                activePointerId = MotionEvent.INVALID_POINTER_ID
                mode = Mode.NONE
                // Tool's onTouch (e.g., adding path to canvasView) should handle invalidation
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // This indicates a multi-touch gesture is starting (could be zoom).
                // Reset the current drawing tool to avoid partial drawings if it's a zoom.
                currentDrawingTool.reset()
                // mode will be set to ZOOM by scaleDetector if it's a pinch-zoom.
                // No need to explicitly set mode here, scaleDetector handles it.
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerIdUp = event.getPointerId(pointerIndex)

                if (pointerIdUp == activePointerId) {
                    // The active pointer (used for dragging/panning) went up.
                    // If there's another pointer down, make it the new active pointer.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) {
                        lastTouchX = event.getX(newPointerIndex)
                        lastTouchY = event.getY(newPointerIndex)
                        activePointerId = event.getPointerId(newPointerIndex)
                        mode = Mode.DRAG // Continue in DRAG mode with the new active pointer
                    } else {
                        // No other pointers, or the new pointer is not valid
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        mode = Mode.NONE
                    }
                }
                // Regardless of which pointer went up, pass the event to the current drawing tool.
                // This might be an ACTION_UP for a multi-touch tool, or could be ignored by single-touch tools.
                // For current tools, if this isn't the last pointer up, it might be best to do nothing or reset.
                // However, the final ACTION_UP for the gesture will handle finalization.
                // Resetting here might be too aggressive if one finger lifts during a multi-touch stroke (not current tools).
                // For now, just pass it. The tool should be robust.
                currentDrawingTool.onTouch(transformedEvent, this)
            }
        }
        transformedEvent.recycle()
        return true
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
 