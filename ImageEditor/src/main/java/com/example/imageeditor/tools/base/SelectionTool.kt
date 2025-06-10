package com.example.imageeditor.tools.base

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.example.imageeditor.domain.LayerManager
import com.example.imageeditor.ui.views.PaintEditorView

class SelectionTool(private val layerManager: LayerManager, private val view: View) : BaseTool(), TransformationCallback {
    private val transformationHandler = TransformationHandler(view)
    private var onItemSelectedListener: ((DrawableItem?) -> Unit)? = null

    init {
        transformationHandler.setTransformationCallback(this)
    }

    fun setOnItemSelectedListener(listener: (DrawableItem?) -> Unit) {
        onItemSelectedListener = listener
    }

    override fun onTouchStart(event: MotionEvent) {
        val allDrawables = layerManager.getLayers()
            .filter { it.isVisible }
            .flatMap { it.getDrawables() }
            .reversed() // Reverse to check top items first
        
        if (transformationHandler.onTouchEvent(event, allDrawables)) {
            onItemSelectedListener?.invoke(transformationHandler.getSelectedItem())
            view.invalidate()
        }
    }
    
    override fun onTouchMove(event: MotionEvent) {
        if (transformationHandler.onTouchEvent(event, emptyList())) {
            view.invalidate()
        }
    }
    
    override fun onTouchEnd(event: MotionEvent) {
        if (transformationHandler.onTouchEvent(event, emptyList())) {
            view.invalidate()
        }
    }
    
    override fun drawPreview(canvas: Canvas) {
        // Save canvas state before drawing
        canvas.save()
        
        // Draw transformation handles and selection
        transformationHandler.draw(canvas)
        
        // Restore canvas state
        canvas.restore()
    }
    
    override fun onToolSelected() {
        // Nothing special needed when tool is selected
    }
    
    override fun onToolDeselected() {
        transformationHandler.setSelectedItem(null)
        view.invalidate()
    }
    
    override fun getSelectedItem(): DrawableItem? = transformationHandler.getSelectedItem()
    
    fun setSelectedItem(item: DrawableItem?) {
        transformationHandler.setSelectedItem(item)
        onItemSelectedListener?.invoke(item)
        view.invalidate()
    }

    override fun onTransformationChanged() {
        if (view is PaintEditorView) {
            view.redrawAllLayers()
        }
        view.invalidate()
    }
} 