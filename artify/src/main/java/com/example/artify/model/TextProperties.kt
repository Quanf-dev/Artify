package com.example.artify.model

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable

data class TextProperties(
    var text: String = "",
    var fontResId: Int = 0, // 0 for default
    var textSizePx: Float = 60f, // Default pixel size
    var alignment: Paint.Align = Paint.Align.CENTER,
    var textColor: Int = Color.WHITE,
    var backgroundColor: Int = Color.TRANSPARENT,
    var backgroundAlpha: Int = 100, // Default 100 (0-255)
    var viewWidth: Int = 0,
    var viewHeight: Int = 0,
    var backgroundMain: Drawable ?= null
)