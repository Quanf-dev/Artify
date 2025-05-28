package com.example.imageeditor.models

import android.graphics.Path

sealed class Shape {
    // Base class for all drawable shapes
    // Common properties like bounding box, etc., could go here
}

data class PathShape(val path: Path, val style: BrushStyle) : Shape()

// Add other shapes as needed, e.g.:
// data class CircleShape(val centerX: Float, val centerY: Float, val radius: Float, val style: BrushStyle) : Shape()
// data class RectShape(val left: Float, val top: Float, val right: Float, val bottom: Float, val style: BrushStyle) : Shape() 