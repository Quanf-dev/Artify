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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class TextGraphic(overlay: GraphicOverlay?) : GraphicOverlay.Graphic(overlay) {
    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    fun updateText(c: Int) {
        postInvalidate()
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */

    override fun draw(canvas: Canvas?) {
        val df: DateFormat = SimpleDateFormat("HH:mm")
        val date = df.format(Calendar.getInstance().getTime())
        val cc: Bitmap = drawTextBitmap(date, Color.WHITE, 100, 150, false, 500, 500)
        canvas?.drawBitmap(cc, 0f, 10f, Paint())
    }

    companion object {
        fun drawTextBitmap(
            string: String,
            color: Int,
            alpha: Int,
            size: Int,
            underline: Boolean,
            width: Int,
            height: Int
        ): Bitmap {
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            //canvas.drawBitmap();
            val paint = Paint()
            paint.setColor(color)
            paint.setAlpha(alpha)
            paint.setTextSize(size.toFloat())
            paint.setAntiAlias(true)
            paint.setUnderlineText(underline)
            canvas.drawText(string, 100f, 150f, paint)
            return result
        }
    }
}
