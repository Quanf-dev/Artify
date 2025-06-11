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

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
internal class FaceGraphic(overlay: GraphicOverlay?, c: Int) : GraphicOverlay.Graphic(overlay) {
    private val TAG = "FaceGraphic"
    @Volatile
    private var mFace: Face? = null
    private var mFaceId = 0
    private var bitmap: Bitmap
    private val graphicOverlay = overlay
    private var currentFilterType = c

    init {
        try {
            bitmap = BitmapFactory.decodeResource(
                graphicOverlay?.context?.resources,
                MASK[c]
            )
            Log.d(TAG, "Filter initialized with type: $c")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading filter bitmap: ${e.message}", e)
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    fun setId(id: Int) {
        mFaceId = id
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    fun updateFace(face: Face, c: Int) {
        mFace = face
        currentFilterType = c

        // Load the bitmap for the new filter type
        try {
            bitmap = BitmapFactory.decodeResource(
                graphicOverlay?.context?.resources,
                MASK[c]
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading filter bitmap for update: ${e.message}", e)
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        postInvalidate()
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    override fun draw(canvas: Canvas?) {
        if (canvas == null) return
        val face = mFace ?: return

        // Use a when statement to handle different filter types with landmark-based positioning
        when (currentFilterType) {
            1, 13, 14 -> drawHat(canvas, face)
            in 4..7 -> drawGlasses(canvas, face)
            in 8..9 -> drawMouthMask(canvas, face)
            10, 11, 12 -> drawNoseFilter(canvas, face)
            else -> drawDefaultFilter(canvas, face)
        }
    }

    private fun drawGlasses(canvas: Canvas, face: Face) {
        val leftEye = getLandmarkPosition(Landmark.LEFT_EYE) ?: return
        val rightEye = getLandmarkPosition(Landmark.RIGHT_EYE) ?: return

        val eyeCenter = PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        val eyeDistance = Math.sqrt(((rightEye.x - leftEye.x).toDouble().pow(2) + (rightEye.y - leftEye.y).toDouble().pow(2))).toFloat()

        val filterWidth = eyeDistance * 3.2f // Tăng hệ số để kính to hơn
        val scale = filterWidth / bitmap.width
        val filterHeight = bitmap.height * scale * 1.2f // Tăng chiều cao kính lên 20%

        val scaledFilterWidth = scaleX(filterWidth)
        val scaledFilterHeight = scaleY(filterHeight)
        val centerX = translateX(eyeCenter.x)
        val centerY = translateY(eyeCenter.y)

        val left = centerX - scaledFilterWidth / 2f
        val top = centerY - scaledFilterHeight / 2f - 90f

        drawRotatedBitmap(canvas, bitmap, left, top, scaledFilterWidth, scaledFilterHeight, face.eulerZ)
    }


    private fun drawNoseFilter(canvas: Canvas, face: Face) {
        val noseBase = getLandmarkPosition(Landmark.NOSE_BASE) ?: return

        val filterWidth = face.width * 0.6f
        val scale = filterWidth / bitmap.width
        val filterHeight = bitmap.height * scale

        val scaledWidth = scaleX(filterWidth)
        val scaledHeight = scaleY(filterHeight)

        val centerX = translateX(noseBase.x)
        val centerY = translateY(noseBase.y)

        val left = centerX - scaledWidth / 2f
        val top = centerY - scaledHeight / 2f

        drawRotatedBitmap(canvas, bitmap, left, top, scaledWidth, scaledHeight, face.eulerZ)
    }

    private fun drawMouthMask(canvas: Canvas, face: Face) {
        val leftEye = getLandmarkPosition(Landmark.LEFT_EYE) ?: return
        val rightEye = getLandmarkPosition(Landmark.RIGHT_EYE) ?: return

        // Dịch lên trên một chút (ở đây là thêm -5% chiều cao khuôn mặt)
        val maskTopY = ((leftEye.y + rightEye.y) / 2f) - (face.height * 0.2f) - 50f

        val faceCenterX = face.position.x + face.width / 2f
        val filterWidth = face.width * 1.2f
        val scale = filterWidth / bitmap.width
        val filterHeight = bitmap.height * scale * 1.2f

        val scaledWidth = scaleX(filterWidth)
        val scaledHeight = scaleY(filterHeight)

        val centerX = translateX(faceCenterX)
        val top = translateY(maskTopY)
        val left = centerX - scaledWidth / 2f

        drawRotatedBitmap(canvas, bitmap, left, top, scaledWidth, scaledHeight, face.eulerZ)
    }


    private fun drawHat(canvas: Canvas, face: Face) {
        val leftEye = getLandmarkPosition(Landmark.LEFT_EYE) ?: return
        val rightEye = getLandmarkPosition(Landmark.RIGHT_EYE) ?: return

        val eyeCenter = PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        // Position the hat lower on the forehead
        val foreheadY = eyeCenter.y - face.height * 0.35f

        val filterWidth = face.width * 1.2f
        val scale = filterWidth / bitmap.width
        val filterHeight = bitmap.height * scale

        val scaledWidth = scaleX(filterWidth)
        val scaledHeight = scaleY(filterHeight)

        val centerX = translateX(eyeCenter.x)
        val centerY = translateY(foreheadY)

        val left = centerX - scaledWidth / 2f
        val top = centerY - scaledHeight / 2f

        drawRotatedBitmap(canvas, bitmap, left, top, scaledWidth, scaledHeight, face.eulerZ)
    }

    private fun drawDefaultFilter(canvas: Canvas, face: Face) {
        val centerX = translateX(face.position.x + face.width / 2)
        val centerY = translateY(face.position.y + face.height / 2)
        val width = scaleX(face.width)
        val height = scaleY(face.height)

        val left = centerX - width / 2f
        val top = centerY - height / 2f

        drawRotatedBitmap(canvas, bitmap, left, top, width, height, face.eulerZ)
    }

    private fun getLandmarkPosition(landmarkId: Int): PointF? {
        return mFace?.landmarks?.find { it.type == landmarkId }?.position
    }

    private fun drawRotatedBitmap(canvas: Canvas, bitmap: Bitmap, left: Float, top: Float, width: Float, height: Float, angle: Float) {
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid bitmap dimensions for drawing")
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
            Log.e(TAG, "Error drawing rotated bitmap", e)
        } finally {
            canvas.restore()
        }
    }

    companion object {
        private val MASK = intArrayOf(
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
