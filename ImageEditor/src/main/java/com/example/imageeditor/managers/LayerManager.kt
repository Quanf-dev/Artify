package com.example.imageeditor.managers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import com.example.imageeditor.models.Layer
import java.util.UUID

class LayerManager(private var initialWidth: Int, private var initialHeight: Int) {
    private val layers = mutableListOf<Layer>()
    private var currentSelectedLayerIndex: Int = -1
    private var defaultBackgroundColor: Int = Color.WHITE

    val currentLayer: Layer?
        get() = layers.getOrNull(currentSelectedLayerIndex)

    val allLayers: List<Layer>
        get() = layers.toList()

    init {
        // Start with a base layer
        if (initialWidth > 0 && initialHeight > 0) {
            addLayer("Background", makeBackground = true)
        }
    }

    fun getCompositeBitmap(): Bitmap {
        val compositeBitmap = Bitmap.createBitmap(initialWidth, initialHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)
        canvas.drawColor(Color.TRANSPARENT) // Start with a transparent composite
        val paint = Paint()
        for (layer in layers) {
            if (layer.isVisible) {
                paint.alpha = (layer.opacity * 255).toInt()
                canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            }
        }
        return compositeBitmap
    }

    fun setDefaultBackgroundColor(color: Int) {
        defaultBackgroundColor = color
        // Potentially update existing background layer if it's pristine
        layers.firstOrNull { it.name == "Background" }?.let {
            val canvas = Canvas(it.bitmap)
            canvas.drawColor(defaultBackgroundColor)
        }
    }

    fun addLayer(name: String = "Layer ${layers.size + 1}", makeBackground: Boolean = false): Layer {
        val newBitmap = Bitmap.createBitmap(initialWidth, initialHeight, Bitmap.Config.ARGB_8888)
        if (makeBackground || (layers.isEmpty() && name == "Background")) {
            val canvas = Canvas(newBitmap)
            canvas.drawColor(defaultBackgroundColor)
        } else {
            // New non-background layers are transparent
            val canvas = Canvas(newBitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }
        val newLayer = Layer(
            id = UUID.randomUUID().toString(),
            name = name,
            bitmap = newBitmap,
            isVisible = true
        )
        layers.add(newLayer)
        currentSelectedLayerIndex = layers.size - 1 // Select the new layer
        return newLayer
    }

    fun deleteLayer(layerId: String): Boolean {
        if (layers.size <= 1) return false // Cannot delete the last layer
        val indexToRemove = layers.indexOfFirst { it.id == layerId }
        if (indexToRemove != -1) {
            layers.removeAt(indexToRemove)
            if (currentSelectedLayerIndex >= indexToRemove) {
                currentSelectedLayerIndex = currentSelectedLayerIndex.coerceAtMost(layers.size - 1)
                if (currentSelectedLayerIndex < 0 && layers.isNotEmpty()) {
                    currentSelectedLayerIndex = 0 // Select first if previous was deleted
                }
            }
            return true
        }
        return false
    }

    fun selectLayer(layerId: String): Boolean {
        val indexToSelect = layers.indexOfFirst { it.id == layerId }
        if (indexToSelect != -1) {
            currentSelectedLayerIndex = indexToSelect
            return true
        }
        return false
    }

    fun moveLayer(layerId: String, newPosition: Int): Boolean {
        val currentPosition = layers.indexOfFirst { it.id == layerId }
        if (currentPosition == -1 || newPosition < 0 || newPosition >= layers.size) {
            return false
        }
        val layer = layers.removeAt(currentPosition)
        layers.add(newPosition, layer)
        // Update currentSelectedLayerIndex if the moved layer was selected or its move affected the selected index
        if (currentSelectedLayerIndex == currentPosition) {
            currentSelectedLayerIndex = newPosition
        } else if (currentPosition < currentSelectedLayerIndex && newPosition >= currentSelectedLayerIndex) {
            currentSelectedLayerIndex--
        } else if (currentPosition > currentSelectedLayerIndex && newPosition <= currentSelectedLayerIndex) {
            currentSelectedLayerIndex++
        }
        return true
    }

    fun setLayerVisibility(layerId: String, isVisible: Boolean): Boolean {
        layers.find { it.id == layerId }?.let {
            it.isVisible = isVisible
            return true
        }
        return false
    }

    fun setLayerOpacity(layerId: String, opacity: Float): Boolean {
        layers.find { it.id == layerId }?.let {
            it.opacity = opacity.coerceIn(0f, 1f)
            return true
        }
        return false
    }

    fun setLayerName(layerId: String, name: String): Boolean {
        layers.find { it.id == layerId }?.let {
            it.name = name
            return true
        }
        return false
    }

    // Merges the specified layer (source) onto the layer below it (destination).
    // The source layer is then removed.

    fun resize(newWidth: Int, newHeight: Int) {
        initialWidth = newWidth
        initialHeight = newHeight
        // This is a destructive operation if content scaling is not handled.
        // For simplicity, we create new bitmaps. For a real app, scaling existing content is better.
        layers.forEach { layer ->
            val oldBitmap = layer.bitmap
            val newBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(newBitmap)
            // Draw old bitmap onto new, scaled if necessary (or centered)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            // Simple center and scale-to-fit logic (maintaining aspect ratio)
            val scaleX = newWidth.toFloat() / oldBitmap.width
            val scaleY = newHeight.toFloat() / oldBitmap.height
            val scale = minOf(scaleX, scaleY)
            val newBitmapWidth = oldBitmap.width * scale
            val newBitmapHeight = oldBitmap.height * scale
            val left = (newWidth - newBitmapWidth) / 2f
            val top = (newHeight - newBitmapHeight) / 2f

            if (layer.name == "Background" && layers.firstOrNull() == layer) { // Check if it IS the first layer
                 canvas.drawColor(Color.WHITE) // Keep background white if it was the default
            } else {
                canvas.drawBitmap(oldBitmap, null, RectF(left, top, left + newBitmapWidth, top + newBitmapHeight), paint)
            }
            
            layer.bitmap = newBitmap
            oldBitmap.recycle() 
        }
    }

    // Helpers for project loading
    public fun clearAllLayersForLoad() {
        layers.forEach { it.bitmap.recycle() } // recycle existing bitmaps
        layers.clear()
        currentSelectedLayerIndex = -1
    }

    public fun addLoadedLayer(layer: Layer) {
        layers.add(layer)
        // Don't automatically select here; loadProject will handle selection.
    }

    public fun ensureBaseLayerIfEmpty() {
        if (layers.isEmpty()) {
            // Add a default background layer if loading resulted in an empty layer list
            val bgBitmap = Bitmap.createBitmap(initialWidth, initialHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bgBitmap)
            canvas.drawColor(defaultBackgroundColor) // Use the potentially customized default color
            val backgroundLayer = Layer(
                id = UUID.randomUUID().toString(),
                name = "Background",
                bitmap = bgBitmap,
                isVisible = true
            )
            layers.add(backgroundLayer)
            currentSelectedLayerIndex = 0
        }
    }

    // Explicitly making updateLayerBitmap public just in case 'internal' causes resolution issues
    // in the current project/IDE state, although it normally shouldn't within the same module.
    public fun updateLayerBitmap(layerId: String, newBitmap: Bitmap) {
        layers.find { it.id == layerId }?.let {
            val canvas = Canvas(it.bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) 
            canvas.drawBitmap(newBitmap, 0f, 0f, null) 
        }
    }
}
 