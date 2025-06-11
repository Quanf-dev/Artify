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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import java.io.IOException

class CameraSourcePreview(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private val TAG = "CameraSourcePreview"
    private val mSurfaceView: SurfaceView
    private var mStartRequested = false
    private var mSurfaceAvailable = false
    private var mCameraSource: CameraSource? = null
    private var mOverlay: GraphicOverlay? = null

    init {
        mSurfaceView = SurfaceView(context)
        mSurfaceView.holder.addCallback(SurfaceCallback())
        addView(mSurfaceView)
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) {
            stop()
            return
        }

        mCameraSource = cameraSource
        mStartRequested = true
        startIfReady()
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource?, overlay: GraphicOverlay?) {
        mOverlay = overlay
        start(cameraSource)
    }

    fun stop() {
        mCameraSource?.stop()
    }

    fun release() {
        mCameraSource?.release()
        mCameraSource = null
    }

    @Throws(IOException::class)
    private fun startIfReady() {
        if (mStartRequested && mSurfaceAvailable) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            
            try {
                Log.d(TAG, "Starting camera source")
                mCameraSource?.start(mSurfaceView.holder)
                
                mCameraSource?.let { source ->
                    val size = source.previewSize
                    val min = Math.min(size.width, size.height)
                    val max = Math.max(size.width, size.height)
                    
                    mOverlay?.let { overlay ->
                        if (isPortraitMode) {
                            // Swap width and height sizes when in portrait
                            overlay.setCameraInfo(min, max, source.cameraFacing)
                        } else {
                            overlay.setCameraInfo(max, min, source.cameraFacing)
                        }
                        overlay.clear()
                    }
                }
                
                mStartRequested = false
                Log.d(TAG, "Camera source started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(TAG, "Surface created")
            mSurfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.d(TAG, "Surface destroyed")
            mSurfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.d(TAG, "Surface changed: $width x $height")
            // No specific action needed
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "onLayout: $left, $top, $right, $bottom")
        
        val width = right - left
        val height = bottom - top
        
        // Just center the SurfaceView in the available space
        val childLeft = 0
        val childTop = 0
        
        for (i in 0 until childCount) {
            getChildAt(i).layout(childLeft, childTop, width, height)
        }

        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }
    }

    private val isPortraitMode: Boolean
        get() {
            val orientation = context.resources.configuration.orientation
            return orientation == Configuration.ORIENTATION_PORTRAIT
        }
}
