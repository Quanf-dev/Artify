
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
        // Để trống vì không cần xử lý color filter trong phiên bản hiện tại
        android.util.Log.d(TAG, "Color filter request ignored")
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
            
            // Save canvas state
            val saveCount = canvas.save()
            
            try {
                // Draw graphics first
                for (graphic in mGraphics) {
                    graphic.draw(canvas)
                }
                

            } finally {
                // Restore canvas state
                canvas.restoreToCount(saveCount)
            }
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
