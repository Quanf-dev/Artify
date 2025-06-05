package com.example.imageeditor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.example.imageeditor.R
import java.util.Stack

/**
 * Manager class for handling image frame templates
 */
class FrameTemplateManager(private val context: Context) {
    
    // Original bitmap (kept for reference)
    private var originalBitmap: Bitmap? = null
    
    // Currently applied frame
    private var currentFrameId: Int = -1
    
    // Last applied bitmap with frame
    private var currentBitmap: Bitmap? = null
    
    // Frame history for undo/redo
    private val undoStack = Stack<FrameState>()
    private val redoStack = Stack<FrameState>()
    
    /**
     * Set the original bitmap to work with
     */
    fun setOriginalBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        clearHistory()
        
        // Save initial state with no frame
        undoStack.push(FrameState(originalBitmap!!.copy(Bitmap.Config.ARGB_8888, true), -1))
    }
    
    /**
     * Get list of available frame templates
     */
    fun getFrameTemplates(): List<FrameTemplate> {
        val frames = mutableListOf<FrameTemplate>()
        
        // Add "No Frame" option
        frames.add(FrameTemplate("No Frame", -1, null))
        
        // Add built-in frames
        frames.add(FrameTemplate("Classic", R.drawable.frame_classic, FrameStyle.OVERLAY))
        frames.add(FrameTemplate("Wooden", R.drawable.frame_wooden, FrameStyle.OVERLAY))
        frames.add(FrameTemplate("Modern", R.drawable.frame_modern, FrameStyle.OVERLAY))
        frames.add(FrameTemplate("Holiday", R.drawable.frame_holiday, FrameStyle.OVERLAY))
        frames.add(FrameTemplate("Polaroid", R.drawable.frame_polaroid, FrameStyle.MASK))
        frames.add(FrameTemplate("Circle", R.drawable.frame_circle, FrameStyle.MASK))
        frames.add(FrameTemplate("Heart", R.drawable.frame_heart, FrameStyle.MASK))
        frames.add(FrameTemplate("Star", R.drawable.frame_star, FrameStyle.MASK))
        frames.add(FrameTemplate("Film", R.drawable.frame_film, FrameStyle.OVERLAY))
        
        return frames
    }
    
    /**
     * Get frame previews for each template using a sample image
     */
    fun getFramePreviews(sampleBitmap: Bitmap?): List<FramePreview> {
        if (sampleBitmap == null) return emptyList()
        
        val previews = mutableListOf<FramePreview>()
        val previewBitmap = createPreviewBitmap(sampleBitmap, 200)
        
        getFrameTemplates().forEach { template ->
            if (template.frameResourceId == -1) {
                // No frame
                previews.add(FramePreview(template, previewBitmap.copy(Bitmap.Config.ARGB_8888, true)))
            } else {
                // Apply frame to preview
                val frameBitmap = applyFrameToImage(previewBitmap, template)
                previews.add(FramePreview(template, frameBitmap))
            }
        }
        
        return previews
    }
    
    /**
     * Apply a frame template to the original image
     */
    fun applyFrame(template: FrameTemplate): Bitmap? {
        if (originalBitmap == null) return null
        
        val resultBitmap = if (template.frameResourceId == -1) {
            // No frame, return original
            originalBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            // Apply frame
            applyFrameToImage(originalBitmap!!, template)
        }
        
        // Save state
        currentFrameId = template.frameResourceId
        currentBitmap = resultBitmap
        
        // Add to history
        undoStack.push(FrameState(resultBitmap, template.frameResourceId))
        redoStack.clear()
        
        return resultBitmap
    }
    
    /**
     * Apply frame to an image
     */
    private fun applyFrameToImage(image: Bitmap, template: FrameTemplate): Bitmap {
        // Create result bitmap with same size as original
        val result = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        when (template.frameStyle) {
            FrameStyle.OVERLAY -> {
                // First draw image
                canvas.drawBitmap(image, 0f, 0f, null)
                
                // Then draw frame on top
                val frameDrawable = ContextCompat.getDrawable(context, template.frameResourceId)
                frameDrawable?.let {
                    it.setBounds(0, 0, image.width, image.height)
                    it.draw(canvas)
                }
            }
            
            FrameStyle.MASK -> {
                // For mask frames, we need to draw image inside mask area
                
                // Get frame as bitmap
                val frameDrawable = ContextCompat.getDrawable(context, template.frameResourceId)
                val frameBitmap = if (frameDrawable is BitmapDrawable) {
                    frameDrawable.bitmap
                } else {
                    // Convert drawable to bitmap if it's not already
                    val maskBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    val maskCanvas = Canvas(maskBitmap)
                    frameDrawable?.setBounds(0, 0, image.width, image.height)
                    frameDrawable?.draw(maskCanvas)
                    maskBitmap
                }
                
                // First draw the frame
                val scaledFrame = Bitmap.createScaledBitmap(frameBitmap, image.width, image.height, true)
                canvas.drawBitmap(scaledFrame, 0f, 0f, null)
                
                // Calculate image placement (center image in frame, respecting aspect ratio)
                val maskPaint = Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                }
                
                // Calculate dimensions to maintain aspect ratio
                val srcRect = RectF(0f, 0f, image.width.toFloat(), image.height.toFloat())
                val dstRect = RectF(0f, 0f, result.width.toFloat(), result.height.toFloat())
                
                // Apply padding (10% of frame size)
                val padding = (dstRect.width() * 0.1f).toInt()
                dstRect.left += padding
                dstRect.top += padding
                dstRect.right -= padding
                dstRect.bottom -= padding
                
                // Create a separate bitmap for the masked image
                val maskedImage = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
                val maskedCanvas = Canvas(maskedImage)
                
                // Draw scaled image in the destination rectangle
                maskedCanvas.drawBitmap(image, null, dstRect, null)
                
                // Apply mask using PorterDuff
                val maskPaint2 = Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                maskedCanvas.drawBitmap(scaledFrame, 0f, 0f, maskPaint2)
                
                // Draw the masked image onto the final canvas
                canvas.drawBitmap(maskedImage, 0f, 0f, null)
            }

            null -> TODO()
        }
        
        return result
    }
    
    /**
     * Create a scaled preview bitmap
     */
    private fun createPreviewBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val ratio = width.toFloat() / height.toFloat()
        
        val targetWidth: Int
        val targetHeight: Int
        
        if (width > height) {
            targetWidth = maxSize
            targetHeight = (maxSize / ratio).toInt()
        } else {
            targetHeight = maxSize
            targetWidth = (maxSize * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
    
    /**
     * Get current framed bitmap
     */
    fun getCurrentBitmap(): Bitmap? {
        return currentBitmap
    }
    
    /**
     * Undo last frame action
     */
    fun undo(): Bitmap? {
        if (undoStack.size > 1) {
            // Move current state to redo stack
            redoStack.push(undoStack.pop())
            
            // Get previous state
            val previousState = undoStack.peek()
            currentFrameId = previousState.frameId
            currentBitmap = previousState.bitmap
            
            return previousState.bitmap
        }
        return null
    }
    
    /**
     * Redo last undone frame action
     */
    fun redo(): Bitmap? {
        if (redoStack.isNotEmpty()) {
            val redoState = redoStack.pop()
            undoStack.push(redoState)
            
            currentFrameId = redoState.frameId
            currentBitmap = redoState.bitmap
            
            return redoState.bitmap
        }
        return null
    }
    
    /**
     * Clear frame history
     */
    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }
}

/**
 * Data class to store frame state
 */
data class FrameState(
    val bitmap: Bitmap,
    val frameId: Int
)

/**
 * Data class to represent a frame template
 */
data class FrameTemplate(
    val name: String,
    val frameResourceId: Int,
    val frameStyle: FrameStyle? = null
)

/**
 * Data class to represent a frame preview
 */
data class FramePreview(
    val template: FrameTemplate,
    val previewBitmap: Bitmap
)

/**
 * Enum to define different frame styles
 */
enum class FrameStyle {
    OVERLAY,  // Frame overlays the image
    MASK      // Frame acts as a mask/shape for the image
} 