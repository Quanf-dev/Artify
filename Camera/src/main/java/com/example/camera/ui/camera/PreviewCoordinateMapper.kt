package com.example.camera.ui.camera

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.view.PreviewView
import timber.log.Timber

class PreviewCoordinateMapper {
    
    private var previewView: PreviewView? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    
    fun setPreviewView(previewView: PreviewView) {
        this.previewView = previewView
    }
    
    fun setImageDimensions(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height
        Timber.d("Image dimensions set: ${width}x${height}")
    }
    
    fun mapPoint(point: PointF): PointF? {
        val preview = previewView ?: return null
        
        val viewWidth = preview.width.toFloat()
        val viewHeight = preview.height.toFloat()
        
        if (viewWidth <= 0 || viewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return null
        }
        
        // Calculate the scale and offset based on preview view's scale type
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspectRatio = viewWidth / viewHeight
        
        val scale: Float
        val offsetX: Float
        val offsetY: Float
        
        if (imageAspectRatio > viewAspectRatio) {
            // Image is wider, fit to width with letterboxing top/bottom
            scale = viewWidth / imageWidth
            offsetX = 0f
            offsetY = (viewHeight - imageHeight * scale) / 2f
        } else {
            // Image is taller, fit to height with letterboxing left/right
            scale = viewHeight / imageHeight
            offsetX = (viewWidth - imageWidth * scale) / 2f
            offsetY = 0f
        }
        
        val mappedX = point.x * scale + offsetX
        val mappedY = point.y * scale + offsetY
        
        return PointF(mappedX, mappedY)
    }
    
    fun mapRect(rect: Rect): RectF? {
        val topLeft = mapPoint(PointF(rect.left.toFloat(), rect.top.toFloat())) ?: return null
        val bottomRight = mapPoint(PointF(rect.right.toFloat(), rect.bottom.toFloat())) ?: return null
        
        return RectF(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }
    
    fun getScaleFactor(): Float {
        val preview = previewView ?: return 1f
        
        val viewWidth = preview.width.toFloat()
        val viewHeight = preview.height.toFloat()
        
        if (viewWidth <= 0 || viewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return 1f
        }
        
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspectRatio = viewWidth / viewHeight
        
        return if (imageAspectRatio > viewAspectRatio) {
            viewWidth / imageWidth
        } else {
            viewHeight / imageHeight
        }
    }
} 