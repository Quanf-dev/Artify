package com.example.imageeditor.utils

import android.content.Context
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
import java.util.*

/**
 * Class that manages image filters using GPUImage library
 */
class ImageFilterManager(private val context: Context) {

    // Original bitmap (kept for reference)
    private var originalBitmap: Bitmap? = null

    // GPUImage instance
    private val gpuImage = GPUImage(context)

    // Currently applied filter
    private var currentFilter: GPUImageFilter = GPUImageFilter()

    // Filter history for undo/redo
    private val undoStack = Stack<Bitmap>()
    private val redoStack = Stack<Bitmap>()

    /**
     * Set the original bitmap to work with
     */
    fun setOriginalBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        gpuImage.setImage(originalBitmap)
        undoStack.clear()
        redoStack.clear()
        
        // Save initial state
        undoStack.push(gpuImage.bitmapWithFilterApplied)
    }

    /**
     * Get all available filters with previews
     */
    fun getFilterPreviews(): List<FilterPreview> {
        if (originalBitmap == null) return emptyList()

        // Create a small preview bitmap for efficiency
        val previewBitmap = createPreviewBitmap(originalBitmap!!, 200)

        // Create GPUImage for preview
        val previewGpuImage = GPUImage(context)
        previewGpuImage.setImage(previewBitmap)

        val filters = mutableListOf<FilterPreview>()
        
        // Normal
        val normalFilter = GPUImageFilter()
        previewGpuImage.setFilter(normalFilter)
        filters.add(FilterPreview("Normal", previewGpuImage.bitmapWithFilterApplied, normalFilter))
        
        // Sepia
        val sepiaFilter = GPUImageSepiaToneFilter()
        previewGpuImage.setFilter(sepiaFilter)
        filters.add(FilterPreview("Sepia", previewGpuImage.bitmapWithFilterApplied, sepiaFilter))
        
        // Grayscale
        val grayscaleFilter = GPUImageGrayscaleFilter()
        previewGpuImage.setFilter(grayscaleFilter)
        filters.add(FilterPreview("Grayscale", previewGpuImage.bitmapWithFilterApplied, grayscaleFilter))
        
        // Invert
        val invertFilter = GPUImageColorInvertFilter()
        previewGpuImage.setFilter(invertFilter)
        filters.add(FilterPreview("Invert", previewGpuImage.bitmapWithFilterApplied, invertFilter))
        
        // Vintage
        val vintageFilter = createVintageFilter()
        previewGpuImage.setFilter(vintageFilter)
        filters.add(FilterPreview("Vintage", previewGpuImage.bitmapWithFilterApplied, vintageFilter))
        
        // Sweet
        val sweetFilter = createSweetFilter()
        previewGpuImage.setFilter(sweetFilter)
        filters.add(FilterPreview("Sweet", previewGpuImage.bitmapWithFilterApplied, sweetFilter))
        
        // Cool
        val coolFilter = createCoolFilter()
        previewGpuImage.setFilter(coolFilter)
        filters.add(FilterPreview("Cool", previewGpuImage.bitmapWithFilterApplied, coolFilter))
        
        // Warm
        val warmFilter = createWarmFilter()
        previewGpuImage.setFilter(warmFilter)
        filters.add(FilterPreview("Warm", previewGpuImage.bitmapWithFilterApplied, warmFilter))
        
        // Sketch
        val sketchFilter = GPUImageSketchFilter()
        previewGpuImage.setFilter(sketchFilter)
        filters.add(FilterPreview("Sketch", previewGpuImage.bitmapWithFilterApplied, sketchFilter))
        
        // Toon
        val toonFilter = GPUImageToonFilter()
        previewGpuImage.setFilter(toonFilter)
        filters.add(FilterPreview("Toon", previewGpuImage.bitmapWithFilterApplied, toonFilter))
        
        // Posterize
        val posterizeFilter = GPUImagePosterizeFilter()
        previewGpuImage.setFilter(posterizeFilter)
        filters.add(FilterPreview("Posterize", previewGpuImage.bitmapWithFilterApplied, posterizeFilter))
        
        // Pixelate
        val pixelateFilter = GPUImagePixelationFilter()
        previewGpuImage.setFilter(pixelateFilter)
        filters.add(FilterPreview("Pixelate", previewGpuImage.bitmapWithFilterApplied, pixelateFilter))
        
        // High Contrast
        val highContrastFilter = createHighContrastFilter()
        previewGpuImage.setFilter(highContrastFilter)
        filters.add(FilterPreview("High Contrast", previewGpuImage.bitmapWithFilterApplied, highContrastFilter))
        
        // Vibrant
        val vibrantFilter = createVibrantFilter()
        previewGpuImage.setFilter(vibrantFilter)
        filters.add(FilterPreview("Vibrant", previewGpuImage.bitmapWithFilterApplied, vibrantFilter))
        
        // Muted
        val mutedFilter = createMutedFilter()
        previewGpuImage.setFilter(mutedFilter)
        filters.add(FilterPreview("Muted", previewGpuImage.bitmapWithFilterApplied, mutedFilter))
        
        // Dreamy
        val dreamyFilter = createDreamyFilter()
        previewGpuImage.setFilter(dreamyFilter)
        filters.add(FilterPreview("Dreamy", previewGpuImage.bitmapWithFilterApplied, dreamyFilter))
        
        return filters
    }

    /**
     * Apply a filter to the image
     */
    fun applyFilter(filter: GPUImageFilter): Bitmap? {
        if (originalBitmap == null) return null

        // Apply new filter
        currentFilter = filter
        gpuImage.setFilter(filter)
        
        // Get result
        val resultBitmap = gpuImage.bitmapWithFilterApplied
        
        // Save to history
        undoStack.push(resultBitmap)
        redoStack.clear()
        
        return resultBitmap
    }

    /**
     * Undo last filter action
     */
    fun undo(): Bitmap? {
        if (undoStack.size > 1) {
            // Move current state to redo stack
            redoStack.push(undoStack.pop())
            
            // Get previous state
            return undoStack.peek()
        }
        return null
    }

    /**
     * Redo last undone filter action
     */
    fun redo(): Bitmap? {
        if (redoStack.isNotEmpty()) {
            val redoState = redoStack.pop()
            undoStack.push(redoState)
            return redoState
        }
        return null
    }

    /**
     * Get current filtered bitmap
     */
    fun getCurrentBitmap(): Bitmap? {
        if (originalBitmap == null) return null
        return gpuImage.bitmapWithFilterApplied
    }

    /**
     * Reset all filters
     */
    fun resetFilters(): Bitmap? {
        if (originalBitmap == null) return null
        
        currentFilter = GPUImageFilter()
        gpuImage.setFilter(currentFilter)
        
        val resultBitmap = gpuImage.bitmapWithFilterApplied
        undoStack.clear()
        redoStack.clear()
        undoStack.push(resultBitmap)
        
        return resultBitmap
    }

    // Helper methods to create custom filters
    
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
    
    private fun createVintageFilter(): GPUImageFilter {
        val filter = GPUImageColorMatrixFilter()
        filter.setColorMatrix(floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.4f, 0.8f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.8f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        ))
        return filter
    }
    
    private fun createSweetFilter(): GPUImageFilter {
        val filter = GPUImageColorMatrixFilter()
        filter.setColorMatrix(floatArrayOf(
            1.2f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.8f, 0.1f,
            0.0f, 0.0f, 0.0f, 1.0f
        ))
        return filter
    }
    
    private fun createCoolFilter(): GPUImageFilter {
        val filter = GPUImageColorMatrixFilter()
        filter.setColorMatrix(floatArrayOf(
            0.8f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.9f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.2f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        ))
        return filter
    }
    
    private fun createWarmFilter(): GPUImageFilter {
        val filter = GPUImageColorMatrixFilter()
        filter.setColorMatrix(floatArrayOf(
            1.1f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.8f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        ))
        return filter
    }
    
    private fun createHighContrastFilter(): GPUImageFilter {
        val filter = GPUImageContrastFilter()
        filter.setContrast(2.0f)
        return filter
    }
    
    private fun createVibrantFilter(): GPUImageFilter {
        val filter = GPUImageFilterGroup()
        filter.addFilter(GPUImageContrastFilter(1.5f))
        filter.addFilter(GPUImageSaturationFilter(1.7f))
        return filter
    }
    
    private fun createMutedFilter(): GPUImageFilter {
        val filter = GPUImageFilterGroup()
        filter.addFilter(GPUImageSaturationFilter(0.7f))
        filter.addFilter(GPUImageBrightnessFilter(-0.1f))
        return filter
    }
    
    private fun createDreamyFilter(): GPUImageFilter {
        val filter = GPUImageFilterGroup()
        filter.addFilter(GPUImageGaussianBlurFilter(0.5f))
        filter.addFilter(GPUImageContrastFilter(1.2f))
        filter.addFilter(GPUImageBrightnessFilter(0.1f))
        return filter
    }
}

/**
 * Data class to represent a filter preview
 */
data class FilterPreview(
    val name: String,
    val previewBitmap: Bitmap,
    val filter: GPUImageFilter
) 