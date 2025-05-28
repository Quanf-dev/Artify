package com.example.imageeditor.models

import android.graphics.Paint

data class StrokeStyle(
    val strokeCap: Paint.Cap = Paint.Cap.ROUND,
    val strokeJoin: Paint.Join = Paint.Join.ROUND,
    val pathEffect: android.graphics.PathEffect? = null
    // Potentially add dash patterns, etc.
) 