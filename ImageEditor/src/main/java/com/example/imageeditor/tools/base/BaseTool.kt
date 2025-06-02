package com.example.imageeditor.tools.base

import android.graphics.Canvas
import android.view.MotionEvent

abstract class BaseTool {
    open fun onTouchStart(event: MotionEvent) {}
    open fun onTouchMove(event: MotionEvent) {}
    open fun onTouchEnd(event: MotionEvent) {}
    open fun onDraw(canvas: Canvas) {}
    open fun createDrawableItem(): DrawableItem? { return null }
    open fun drawPreview(canvas: Canvas) {}
    open fun onToolSelected() {}
    open fun onToolDeselected() {}
    open fun getSelectedItem(): DrawableItem? { return null }
}