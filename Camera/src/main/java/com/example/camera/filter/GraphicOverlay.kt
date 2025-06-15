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

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.android.gms.vision.CameraSource

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview).  The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.
 *
 *
 *
 *
 * Supports scaling and mirroring of the graphics relative the camera's preview properties.  The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.
 *
 *
 *
 *
 * Associated [Graphic] items should use the following methods to convert to view coordinates
 * for the graphics that are drawn:
 *
 *  1. [Graphic.scaleX] and [Graphic.scaleY] adjust the size of the
 * supplied value from the preview scale to the view scale.
 *  1. [Graphic.translateX] and [Graphic.translateY] adjust the coordinate
 * from the preview's coordinate system to the view coordinate system.
 *
 */
class GraphicOverlay(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val TAG = "GraphicOverlay"
    val mLock = Any()
    private var mPreviewWidth = 0
    private var mWidthScaleFactor = 1.0f
    private var mPreviewHeight = 0
    private var mHeightScaleFactor = 1.0f
    private var mFacing = CameraSource.CAMERA_FACING_BACK
    private val mGraphics: MutableSet<Graphic> = HashSet<Graphic>()
    private var isGridEnabled: Boolean = false
    
    // Color filter support
    private var colorFilterPaint: Paint? = null
    private var currentColorFilter: Any? = null

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the [Graphic.draw] method to define the
     * graphics element.  Add instances to the overlay using [GraphicOverlay.add].
     */
    abstract class Graphic(val overlay: GraphicOverlay?) {
        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         *
         *  1. [Graphic.scaleX] and [Graphic.scaleY] adjust the size of
         * the supplied value from the preview scale to the view scale.
         *  1. [Graphic.translateX] and [Graphic.translateY] adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.
         *
         *
         * @param canvas drawing canvas
         */
        abstract fun draw(canvas: Canvas?)

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        fun scaleX(horizontal: Float): Float {
            return horizontal * (overlay?.mWidthScaleFactor ?: 1f )
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        fun scaleY(vertical: Float): Float {
            return vertical * (overlay?.mHeightScaleFactor ?: 1f)
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        fun translateX(x: Float): Float {
            if (overlay?.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return overlay.width - scaleX(x)
            } else {
                return scaleX(x)
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        fun translateY(y: Float): Float {
            return scaleY(y)
        }

        fun postInvalidate() {
            overlay?.postInvalidate()
        }
    }

    /**
     * Removes all graphics from the overlay.
     */
    fun clear() {
        synchronized(mLock) {
            mGraphics.clear()
        }
        postInvalidate()
    }

    /**
     * Adds a graphic to the overlay.
     */
    fun add(graphic: Graphic?) {
        if (graphic == null) return
        
        synchronized(mLock) {
            mGraphics.add(graphic)
        }
        postInvalidate()
    }

    /**
     * Removes a graphic from the overlay.
     */
    fun remove(graphic: Graphic?) {
        if (graphic == null) return
        
        synchronized(mLock) {
            mGraphics.remove(graphic)
        }
        postInvalidate()
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    fun setCameraInfo(previewWidth: Int, previewHeight: Int, facing: Int) {
        synchronized(mLock) {
            mPreviewWidth = previewWidth
            mPreviewHeight = previewHeight
            mFacing = facing
        }
        postInvalidate()
    }

    /**
     * Enables or disables grid display (disabled as requested)
     */
    fun setGridEnabled(enabled: Boolean) {
        // Grid functionality disabled as requested
    }

    /**
     * Sets color filter for the overlay
     */
    fun setColorFilter(colorFilter: Any?) {
        currentColorFilter = colorFilter
        colorFilterPaint = when (colorFilter.toString()) {
            "BLACK_WHITE" -> createBlackWhiteFilter()
            "SEPIA" -> createSepiaFilter()
            "VINTAGE" -> createVintageFilter()
            "COOL" -> createCoolFilter()
            "WARM" -> createWarmFilter()
            else -> null
        }
        postInvalidate()
    }

    private fun createBlackWhiteFilter(): Paint {
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f) // Remove all color saturation
        }
        return Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
    }

    private fun createSepiaFilter(): Paint {
        val colorMatrix = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
    }

    private fun createVintageFilter(): Paint {
        val colorMatrix = ColorMatrix(floatArrayOf(
            0.6f, 0.3f, 0.1f, 0f, 30f,
            0.2f, 0.7f, 0.1f, 0f, 10f,
            0.2f, 0.3f, 0.5f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        ))
        return Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
    }

    private fun createCoolFilter(): Paint {
        val colorMatrix = ColorMatrix(floatArrayOf(
            0.8f, 0.2f, 0.2f, 0f, 0f,
            0.1f, 0.8f, 0.1f, 0f, 0f,
            0.2f, 0.3f, 1.2f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
    }

    private fun createWarmFilter(): Paint {
        val colorMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0.1f, 0.1f, 0f, 0f,
            0.1f, 1.1f, 0.1f, 0f, 0f,
            0.1f, 0.1f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = width.toFloat() / mPreviewWidth.toFloat()
                mHeightScaleFactor = height.toFloat() / mPreviewHeight.toFloat()
            }
            
            // Clear the canvas first to prevent artifacts
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            
            // Save canvas state for color filter
            val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), colorFilterPaint)
            
            for (graphic in mGraphics) {
                graphic.draw(canvas)
            }
            
            // Restore canvas state
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * Gets all graphics in the overlay for external use
     */
    fun getGraphics(): Set<Graphic> {
        synchronized(mLock) {
            return HashSet(mGraphics)
        }
    }
    
    /**
     * Gets the width scale factor
     */
    fun getWidthScaleFactor(): Float {
        return mWidthScaleFactor
    }
    
    /**
     * Gets the height scale factor
     */
    fun getHeightScaleFactor(): Float {
        return mHeightScaleFactor
    }
    
    /**
     * Gets the camera facing
     */
    fun getCameraFacing(): Int {
        return mFacing
    }
}
