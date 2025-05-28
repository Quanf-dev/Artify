package com.example.imageeditor.models

import android.graphics.Bitmap

data class Layer(
    val id: String, // Unique identifier for the layer
    var name: String,
    var bitmap: Bitmap, // The content of the layer
    var isVisible: Boolean = true,
    var opacity: Float = 1.0f, // 0.0f (transparent) to 1.0f (opaque)
    // Add other layer properties like blending mode, etc.
) {
    // Ensure a new bitmap is created if you need to modify it to avoid issues
    // with shared bitmap objects if this layer is duplicated or manipulated.
    fun duplicateBitmap(): Bitmap? {
        return bitmap.config?.let { bitmap.copy(it, true) }
    }
}