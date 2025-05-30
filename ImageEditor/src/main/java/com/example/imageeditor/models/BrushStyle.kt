package com.example.imageeditor.models

import android.graphics.Color
 
data class BrushStyle(
    val color: Int = Color.BLACK,
    val strokeWidth: Float = 5f,
    // Add other brush properties here, e.g., cap, join, path effect
) 