package com.example.imageeditor.tools.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class TransformationHandler(private val view: View) {
    private var selectedItem: DrawableItem? = null
    private var initialTouchPoint: PointF? = null
    private var initialItemPosition: PointF? = null
    private var transformMode: TransformMode = TransformMode.NONE
    private var transformationCallback: TransformationCallback? = null

    private val selectionPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun onTouchEvent(event: MotionEvent, drawables: List<DrawableItem>): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return onTouchStart(event, drawables)
            MotionEvent.ACTION_MOVE -> return onTouchMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return onTouchEnd(event)
        }
        return false
    }

    private fun onTouchStart(event: MotionEvent, drawables: List<DrawableItem>): Boolean {
        val touchPoint = PointF(event.x, event.y)
        initialTouchPoint = touchPoint

        // Check if we're selecting a new item
        val touchedItem = drawables.firstOrNull { it.contains(event.x, event.y) }

        if (touchedItem != null) {
            selectedItem = touchedItem
            initialItemPosition = PointF(touchedItem.position.x, touchedItem.position.y)
            transformMode = TransformMode.MOVE
            return true
        } else {
            selectedItem = null
            transformMode = TransformMode.NONE
            return false
        }
    }

    private fun onTouchMove(event: MotionEvent): Boolean {
        val currentPoint = PointF(event.x, event.y)
        val initialPoint = initialTouchPoint ?: return false

        selectedItem?.let { item ->
            if (transformMode == TransformMode.MOVE) {
                val dx = currentPoint.x - initialPoint.x
                val dy = currentPoint.y - initialPoint.y
                initialItemPosition?.let { initial ->
                    item.position.set(initial.x + dx, initial.y + dy)
                    transformationCallback?.onTransformationChanged()
                }
            }
        }
        return transformMode != TransformMode.NONE
    }

    private fun onTouchEnd(event: MotionEvent): Boolean {
        val wasTransforming = transformMode != TransformMode.NONE
        transformMode = TransformMode.NONE
        initialTouchPoint = null
        initialItemPosition = null
        return wasTransforming
    }

    fun draw(canvas: Canvas) {
        selectedItem?.let { item ->
            val bounds = item.getBounds()
            // Draw selection rectangle only
            canvas.drawRect(bounds, selectionPaint)
        }
    }

    fun getSelectedItem(): DrawableItem? = selectedItem

    fun setSelectedItem(item: DrawableItem?) {
        selectedItem = item
        transformationCallback?.onTransformationChanged()
    }

    fun setTransformationCallback(callback: TransformationCallback) {
        transformationCallback = callback
    }

    private enum class TransformMode {
        NONE,
        MOVE
    }
} 
