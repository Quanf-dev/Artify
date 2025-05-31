package com.example.imageeditor.domain

import android.graphics.Canvas
import com.example.imageeditor.tools.base.DrawableItem

class Layer {
    private val drawables = mutableListOf<DrawableItem>()
    var isVisible: Boolean = true
    var opacity: Int = 255

    fun addDrawable(drawable: DrawableItem) {
        drawables.add(drawable)
    }

    fun removeDrawable(drawable: DrawableItem) {
        drawables.remove(drawable)
    }

    fun getDrawables(): List<DrawableItem> = drawables.toList()

    fun draw(canvas: Canvas) {
        if (!isVisible) return
        drawables.forEach { it.draw(canvas, opacity) }
    }

    fun clear() {
        drawables.clear()
    }

    fun copy(): Layer {
        val newLayer = Layer()
        newLayer.isVisible = this.isVisible
        newLayer.opacity = this.opacity
        drawables.forEach { drawable ->
            newLayer.addDrawable(drawable.copy())
        }
        return newLayer
    }
} 