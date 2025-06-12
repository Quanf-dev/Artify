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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import com.example.camera.R
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.Landmark
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
internal class FaceGraphic(
    overlay: GraphicOverlay?,
    initialFilterType: Int
) : GraphicOverlay.Graphic(overlay) {
    
    private val tag = "FaceGraphic"
    @Volatile
    private var face: Face? = null
    private var faceId: Int = 0
    private var filterBitmap: Bitmap
    private val graphicOverlay = overlay
    private var currentFilterType = initialFilterType

    init {
        filterBitmap = loadFilterBitmap(initialFilterType)
    }

    fun setId(id: Int) {
        faceId = id
    }

    /**
     * Updates the face instance from the detection of the most recent frame.
     */
    fun updateFace(face: Face, filterType: Int) {
        this.face = face
        
        if (currentFilterType != filterType) {
            currentFilterType = filterType
            filterBitmap = loadFilterBitmap(filterType)
        }

        postInvalidate()
    }

    private fun loadFilterBitmap(filterType: Int): Bitmap {
        return try {
            BitmapFactory.decodeResource(
                graphicOverlay?.context?.resources,
                FILTER_DRAWABLES[filterType]
            )
        } catch (e: Exception) {
            Log.e(tag, "Error loading filter bitmap for type $filterType: ${e.message}", e)
            createTransparentBitmap()
        }
    }

    private fun createTransparentBitmap(): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    override fun draw(canvas: Canvas?) {
        if (canvas == null) return
        val detectedFace = face ?: return

        when (currentFilterType) {
            FilterType.HAT.ordinal, 
            FilterType.HAT2.ordinal -> drawHatFilter(canvas, detectedFace)
            
            FilterType.GLASSES2.ordinal,
            FilterType.GLASSES3.ordinal,
            FilterType.GLASSES4.ordinal,
            FilterType.GLASSES5.ordinal -> drawGlassesFilter(canvas, detectedFace)
            
            FilterType.MASK2.ordinal,
            FilterType.MASK3.ordinal -> drawMouthMaskFilter(canvas, detectedFace)
            
            FilterType.DOG.ordinal,
            FilterType.CAT2.ordinal -> drawNoseFilter(canvas, detectedFace)
            
            else -> drawDefaultFilter(canvas, detectedFace)
        }
    }

    private fun drawGlassesFilter(canvas: Canvas, face: Face) {
        val leftEye = getLandmarkPosition(face, Landmark.LEFT_EYE) ?: return
        val rightEye = getLandmarkPosition(face, Landmark.RIGHT_EYE) ?: return

        val eyeCenter = PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        val eyeDistance = calculateDistance(leftEye, rightEye)

        val filterWidth = eyeDistance * GLASSES_SCALE_FACTOR
        val scale = filterWidth / filterBitmap.width
        val filterHeight = filterBitmap.height * scale * GLASSES_HEIGHT_MULTIPLIER

        val scaledFilterWidth = scaleX(filterWidth)
        val scaledFilterHeight = scaleY(filterHeight)
        val centerX = translateX(eyeCenter.x)
        val centerY = translateY(eyeCenter.y)

        val left = centerX - scaledFilterWidth / 2f
        val top = centerY - scaledFilterHeight / 2f - GLASSES_VERTICAL_OFFSET

        drawRotatedBitmap(canvas, filterBitmap, left, top, scaledFilterWidth, scaledFilterHeight, face.eulerZ)
    }

    private fun drawNoseFilter(canvas: Canvas, face: Face) {
        val noseBase = getLandmarkPosition(face, Landmark.NOSE_BASE) ?: return

        val filterWidth = face.width * NOSE_FILTER_SCALE
        val scale = filterWidth / filterBitmap.width
        val filterHeight = filterBitmap.height * scale

        val scaledWidth = scaleX(filterWidth)
        val scaledHeight = scaleY(filterHeight)

        val centerX = translateX(noseBase.x)
        val centerY = translateY(noseBase.y)

        val left = centerX - scaledWidth / 2f
        val top = centerY - scaledHeight / 2f

        drawRotatedBitmap(canvas, filterBitmap, left, top, scaledWidth, scaledHeight, face.eulerZ)
    }

    private fun drawMouthMaskFilter(canvas: Canvas, face: Face) {
        val leftEye = getLandmarkPosition(face, Landmark.LEFT_EYE) ?: return
        val rightEye = getLandmarkPosition(face, Landmark.RIGHT_EYE) ?: return

        val maskTopY = ((leftEye.y + rightEye.y) / 2f) - (face.height * MOUTH_MASK_VERTICAL_OFFSET) - MOUTH_MASK_Y_ADJUSTMENT

        val faceCenterX = face.position.x + face.width / 2f
        val filterWidth = face.width * MOUTH_MASK_SCALE
        val scale = filterWidth / filterBitmap.width
        val filterHeight = filterBitmap.height * scale * MOUTH_MASK_HEIGHT_MULTIPLIER

        val scaledWidth = scaleX(filterWidth)
        val scaledHeight = scaleY(filterHeight)

        val centerX = translateX(faceCenterX)
        val top = translateY(maskTopY)
        val left = centerX - scaledWidth / 2f

        drawRotatedBitmap(canvas, filterBitmap, left, top, scaledWidth, scaledHeight, face.eulerZ)
    }

    private fun drawHatFilter(canvas: Canvas, face: Face) {
        val leftEye = getLandmarkPosition(face, Landmark.LEFT_EYE) ?: return
        val rightEye = getLandmarkPosition(face, Landmark.RIGHT_EYE) ?: return

        val eyeCenter = PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        val foreheadY = eyeCenter.y - face.height * HAT_FOREHEAD_OFFSET

        val filterWidth = face.width * HAT_SCALE
        val scale = filterWidth / filterBitmap.width
        val filterHeight = filterBitmap.height * scale

        val scaledWidth = scaleX(filterWidth)
        val scaledHeight = scaleY(filterHeight)

        val centerX = translateX(eyeCenter.x)
        val centerY = translateY(foreheadY)

        val left = centerX - scaledWidth / 2f
        val top = centerY - scaledHeight / 2f

        drawRotatedBitmap(canvas, filterBitmap, left, top, scaledWidth, scaledHeight, face.eulerZ)
    }

    private fun drawDefaultFilter(canvas: Canvas, face: Face) {
        val centerX = translateX(face.position.x + face.width / 2)
        val centerY = translateY(face.position.y + face.height / 2)
        val width = scaleX(face.width)
        val height = scaleY(face.height)

        val left = centerX - width / 2f
        val top = centerY - height / 2f

        drawRotatedBitmap(canvas, filterBitmap, left, top, width, height, face.eulerZ)
    }

    private fun getLandmarkPosition(face: Face, landmarkType: Int): PointF? {
        return face.landmarks.find { it.type == landmarkType }?.position
    }

    private fun calculateDistance(point1: PointF, point2: PointF): Float {
        return sqrt((point2.x - point1.x).toDouble().pow(2) + (point2.y - point1.y).toDouble().pow(2)).toFloat()
    }

    private fun drawRotatedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        angle: Float
    ) {
        if (width <= 0 || height <= 0) {
            Log.e(tag, "Invalid bitmap dimensions for drawing: width=$width, height=$height")
            return
        }
        
        canvas.save()
        try {
            val centerX = left + width / 2f
            val centerY = top + height / 2f
            canvas.rotate(angle, centerX, centerY)

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), true)
            canvas.drawBitmap(scaledBitmap, left, top, Paint())
        } catch (e: Exception) {
            Log.e(tag, "Error drawing rotated bitmap", e)
        } finally {
            canvas.restore()
        }
    }

    enum class FilterType {
        NO_FILTER, HAIR, OP, SNAP,
        GLASSES2, GLASSES3, GLASSES4, GLASSES5,
        MASK2, MASK3, DOG, CAT2, HAT, HAT2
    }

    companion object {
        // Filter scale factors
        private const val GLASSES_SCALE_FACTOR = 3.2f
        private const val GLASSES_HEIGHT_MULTIPLIER = 1.2f
        private const val GLASSES_VERTICAL_OFFSET = 90f
        
        private const val NOSE_FILTER_SCALE = 0.6f
        
        private const val MOUTH_MASK_SCALE = 1.2f
        private const val MOUTH_MASK_HEIGHT_MULTIPLIER = 1.2f
        private const val MOUTH_MASK_VERTICAL_OFFSET = 0.2f
        private const val MOUTH_MASK_Y_ADJUSTMENT = 50f
        
        private const val HAT_SCALE = 1.2f
        private const val HAT_FOREHEAD_OFFSET = 0.35f

        private val FILTER_DRAWABLES = intArrayOf(
            R.drawable.transparent,
            R.drawable.hair,
            R.drawable.op,
            R.drawable.snap,
            R.drawable.glasses2,
            R.drawable.glasses3,
            R.drawable.glasses4,
            R.drawable.glasses5,
            R.drawable.mouthmask,
            R.drawable.mouthmask2,
            R.drawable.dog,
            R.drawable.cat2,
            R.drawable.hat,
            R.drawable.hat2
        )
    }
}
