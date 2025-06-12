/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.camera.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Graphic instance for rendering text overlays on the camera preview.
 */
class TextGraphic(overlay: GraphicOverlay?) : GraphicOverlay.Graphic(overlay) {
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        alpha = TEXT_ALPHA
        textSize = TEXT_SIZE
        isAntiAlias = true
        isUnderlineText = false
    }

    /**
     * Updates the text graphic.
     */
    fun updateText() {
        postInvalidate()
    }

    /**
     * Draws the text on the supplied canvas.
     */
    override fun draw(canvas: Canvas?) {
        if (canvas == null) return
        
        val currentTime = getCurrentTimeString()
        val textBitmap = createTextBitmap(currentTime)
        canvas.drawBitmap(textBitmap, TEXT_X_POSITION, TEXT_Y_POSITION, null)
    }

    private fun getCurrentTimeString(): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    private fun createTextBitmap(text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, TEXT_DRAW_X, TEXT_DRAW_Y, textPaint)
        return bitmap
    }

    companion object {
        // Text display constants
        private const val TEXT_SIZE = 100f
        private const val TEXT_ALPHA = 150
        private const val TEXT_X_POSITION = 0f
        private const val TEXT_Y_POSITION = 10f
        private const val TEXT_DRAW_X = 100f
        private const val TEXT_DRAW_Y = 150f
        private const val BITMAP_WIDTH = 500
        private const val BITMAP_HEIGHT = 500

        /**
         * Creates a bitmap with custom text properties.
         */
        fun createCustomTextBitmap(
            text: String,
            color: Int = Color.WHITE,
            alpha: Int = 100,
            size: Int = 150,
            underline: Boolean = false,
            width: Int = 500,
            height: Int = 500
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val paint = Paint().apply {
                this.color = color
                this.alpha = alpha
                textSize = size.toFloat()
                isAntiAlias = true
                isUnderlineText = underline
            }
            
            canvas.drawText(text, 100f, 150f, paint)
            return bitmap
        }
    }
}
