package com.example.imageeditor.tools.base

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF

interface DrawableItem {
    val id: String
    var color: Int
    var strokeWidth: Float
    var opacity: Int
    var isFilled: Boolean
    var position: PointF
    var rotation: Float
    
    fun draw(canvas: Canvas, layerOpacity: Int)
    fun contains(x: Float, y: Float): Boolean
    fun getBounds(): RectF
    fun transform(matrix: Matrix)
    fun copy(): DrawableItem
}