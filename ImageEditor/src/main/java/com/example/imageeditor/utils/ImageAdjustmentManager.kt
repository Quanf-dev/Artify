package com.example.imageeditor.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import java.util.Stack

// AdjustmentState lưu bitmap và các tham số
data class AdjustmentState(
    val bitmap: Bitmap,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val exposure: Float,
    val hue: Float,
    val temperature: Float,
    val sharpness: Float,
    val luminance: Float,
    val fade: Float
)

/**
 * Manager class for handling image adjustments like brightness, contrast, saturation, etc.
 */
class ImageAdjustmentManager {
    // Default values
    private var brightness = 0f      // Range: -100 to 100
    private var contrast = 1f        // Range: 0.5 to 1.5
    private var saturation = 1f      // Range: 0 to 2
    private var exposure = 1f        // Range: 0.5 to 1.5
    private var hue = 0f             // Range: -180 to 180
    private var temperature = 0f     // Range: -100 to 100 (negative: cooler, positive: warmer)
    private var sharpness = 0f       // Range: 0 to 100
    private var luminance = 1f       // Range: 0.5 to 1.5
    private var fade = 0f            // Range: 0 to 100

    // Undo/Redo stacks
    private val undoStack = Stack<AdjustmentState>()
    private val redoStack = Stack<AdjustmentState>()

    // Original bitmap (kept for reference)
    private var originalBitmap: Bitmap? = null
    
    // Last applied bitmap
    private var currentBitmap: Bitmap? = null
    
    /**
     * Set the original bitmap to work with
     */
    fun setOriginalBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        clearHistory()
        applyAdjustments(pushToStack = true)
    }
    
    /**
     * Apply all adjustments and return the resulting bitmap
     */
    fun applyAdjustments(pushToStack: Boolean = true): Bitmap? {
        if (originalBitmap == null) return null
        
        var result = originalBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Create color matrix for adjustments
        val colorMatrix = ColorMatrix()
        
        // Apply brightness
        applyBrightness(colorMatrix, brightness)
        
        // Apply contrast
        applyContrast(colorMatrix, contrast)
        
        // Apply saturation
        applySaturation(colorMatrix, saturation)
        
        // Apply exposure
        applyExposure(colorMatrix, exposure)
        
        // Apply hue
        applyHue(colorMatrix, hue)
        
        // Apply temperature
        applyTemperature(colorMatrix, temperature)
        
        // Apply luminance
        applyLuminance(colorMatrix, luminance)
        
        // Apply color matrix to paint
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        // Draw with adjustments
        canvas.drawBitmap(originalBitmap!!, 0f, 0f, paint)
        
        // Apply fade effect (opacity)
        if (fade > 0) {
            applyFade(result, fade)
        }
        
        // Apply sharpness if needed
        if (sharpness > 0) {
            result = applySharpness(result, sharpness)
        }
        
        currentBitmap = result
        if (pushToStack) pushToUndoStack()
        return result
    }

    /**
     * Reset all adjustments to default values
     */
    fun resetAdjustments() {
        brightness = 0f
        contrast = 1f
        saturation = 1f
        exposure = 1f
        hue = 0f
        temperature = 0f
        sharpness = 0f
        luminance = 1f
        fade = 0f
        clearHistory()
        applyAdjustments(pushToStack = true)
    }
    
    /**
     * Set brightness value
     * @param value Range from -100 (darkest) to 100 (brightest)
     */
    fun setBrightness(value: Float, pushToStack: Boolean = true): Bitmap? {
        brightness = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set contrast value
     * @param value Range from 0.5 (low contrast) to 1.5 (high contrast)
     */
    fun setContrast(value: Float, pushToStack: Boolean = true): Bitmap? {
        contrast = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set saturation value
     * @param value Range from 0 (grayscale) to 2 (super saturated)
     */
    fun setSaturation(value: Float, pushToStack: Boolean = true): Bitmap? {
        saturation = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set exposure value
     * @param value Range from 0.5 (underexposed) to 1.5 (overexposed)
     */
    fun setExposure(value: Float, pushToStack: Boolean = true): Bitmap? {
        exposure = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set hue rotation value
     * @param value Range from -180 to 180 degrees
     */
    fun setHue(value: Float, pushToStack: Boolean = true): Bitmap? {
        hue = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set color temperature
     * @param value Range from -100 (cool/blue) to 100 (warm/yellow)
     */
    fun setTemperature(value: Float, pushToStack: Boolean = true): Bitmap? {
        temperature = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set sharpness value
     * @param value Range from 0 (normal) to 100 (very sharp)
     */
    fun setSharpness(value: Float, pushToStack: Boolean = true): Bitmap? {
        sharpness = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set luminance value
     * @param value Range from 0.5 (dark) to 1.5 (bright)
     */
    fun setLuminance(value: Float, pushToStack: Boolean = true): Bitmap? {
        luminance = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Set fade value
     * @param value Range from 0 (no fade) to 100 (maximum fade)
     */
    fun setFade(value: Float, pushToStack: Boolean = true): Bitmap? {
        fade = value
        return applyAdjustments(pushToStack)
    }
    
    /**
     * Get current bitmap with all adjustments applied
     */
    fun getCurrentBitmap(): Bitmap? {
        return currentBitmap
    }
    
    // Helper methods for applying effects
    
    private fun applyBrightness(colorMatrix: ColorMatrix, brightness: Float) {
        // Convert from -100,100 to -255,255 range
        val adjustedBrightness = brightness * 2.55f
        
        val brightnessArray = floatArrayOf(
            1f, 0f, 0f, 0f, adjustedBrightness,
            0f, 1f, 0f, 0f, adjustedBrightness,
            0f, 0f, 1f, 0f, adjustedBrightness,
            0f, 0f, 0f, 1f, 0f
        )
        
        colorMatrix.postConcat(ColorMatrix(brightnessArray))
    }
    
    private fun applyContrast(colorMatrix: ColorMatrix, contrast: Float) {
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f
        
        val contrastArray = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        
        colorMatrix.postConcat(ColorMatrix(contrastArray))
    }
    
    private fun applySaturation(colorMatrix: ColorMatrix, saturation: Float) {
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        colorMatrix.postConcat(saturationMatrix)
    }
    
    private fun applyExposure(colorMatrix: ColorMatrix, exposure: Float) {
        val exposureArray = floatArrayOf(
            exposure, 0f, 0f, 0f, 0f,
            0f, exposure, 0f, 0f, 0f,
            0f, 0f, exposure, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        
        colorMatrix.postConcat(ColorMatrix(exposureArray))
    }
    
    private fun applyHue(colorMatrix: ColorMatrix, hue: Float) {
        val hueRadians = Math.toRadians(hue.toDouble()).toFloat()
        val cosVal = Math.cos(hueRadians.toDouble()).toFloat()
        val sinVal = Math.sin(hueRadians.toDouble()).toFloat()
        
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        
        val hueArray = floatArrayOf(
            lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        
        colorMatrix.postConcat(ColorMatrix(hueArray))
    }
    
    private fun applyTemperature(colorMatrix: ColorMatrix, temperature: Float) {
        // Convert from -100,100 range to 0.8,1.2 for blue and red
        val normalized = (temperature + 100) / 200f  // 0 to 1
        
        // Blue for cool (higher when temp is lower)
        val blue = 1.2f - (normalized * 0.4f)  // 1.2 to 0.8
        
        // Red for warm (higher when temp is higher)
        val red = 0.8f + (normalized * 0.4f)   // 0.8 to 1.2
        
        val temperatureArray = floatArrayOf(
            red, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, blue, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        
        colorMatrix.postConcat(ColorMatrix(temperatureArray))
    }
    
    private fun applyLuminance(colorMatrix: ColorMatrix, luminance: Float) {
        val luminanceArray = floatArrayOf(
            luminance, 0f, 0f, 0f, 0f,
            0f, luminance, 0f, 0f, 0f,
            0f, 0f, luminance, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        
        colorMatrix.postConcat(ColorMatrix(luminanceArray))
    }
    
    private fun applyFade(bitmap: Bitmap, fade: Float) {
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // Convert from 0-100 to 0-255 for alpha
        val alpha = 255 - (fade * 2.55f).toInt()
        
        paint.alpha = alpha
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    }
    
    private fun applySharpness(bitmap: Bitmap, sharpness: Float): Bitmap {
        // Create a copy of the input bitmap
        val output = bitmap.copy(bitmap.config!!, true)
        
        // Convert sharpness to a usable value (0-2)
        val amount = sharpness / 50f
        
        // Only apply sharpness if amount > 0
        if (amount > 0) {
            val width = bitmap.width
            val height = bitmap.height
            
            // Create 3x3 kernel for convolution
            val kernel = floatArrayOf(
                0f, -amount, 0f,
                -amount, 1 + 4 * amount, -amount,
                0f, -amount, 0f
            )
            
            // Apply convolution with kernel
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val result = IntArray(width * height)
            
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var red = 0f
                    var green = 0f
                    var blue = 0f
                    var alpha = 0f
                    
                    // Apply kernel
                    for (ky in -1..1) {
                        for (kx in -1..1) {
                            val pixel = pixels[(y + ky) * width + (x + kx)]
                            val kernelValue = kernel[(ky + 1) * 3 + (kx + 1)]
                            
                            alpha += ((pixel shr 24) and 0xff) * kernelValue
                            red += ((pixel shr 16) and 0xff) * kernelValue
                            green += ((pixel shr 8) and 0xff) * kernelValue
                            blue += (pixel and 0xff) * kernelValue
                        }
                    }
                    
                    // Clamp values
                    alpha = alpha.coerceIn(0f, 255f)
                    red = red.coerceIn(0f, 255f)
                    green = green.coerceIn(0f, 255f)
                    blue = blue.coerceIn(0f, 255f)
                    
                    result[y * width + x] = (alpha.toInt() shl 24) or
                            (red.toInt() shl 16) or
                            (green.toInt() shl 8) or
                            blue.toInt()
                }
            }
            
            output.setPixels(result, 0, width, 0, 0, width, height)
        }
        
        return output
    }

    private fun pushToUndoStack() {
        val state = AdjustmentState(
            currentBitmap!!.copy(Bitmap.Config.ARGB_8888, true),
            brightness, contrast, saturation, exposure, hue, temperature, sharpness, luminance, fade
        )
        undoStack.push(state)
        redoStack.clear()
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    fun undo(): AdjustmentState? {
        if (undoStack.size > 1) {
            redoStack.push(undoStack.pop())
            val prev = undoStack.peek()
            restoreState(prev)
            return prev
        }
        return null
    }

    fun redo(): AdjustmentState? {
        if (redoStack.isNotEmpty()) {
            val redoState = redoStack.pop()
            undoStack.push(redoState)
            restoreState(redoState)
            return redoState
        }
        return null
    }

    private fun restoreState(state: AdjustmentState) {
        currentBitmap = state.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        brightness = state.brightness
        contrast = state.contrast
        saturation = state.saturation
        exposure = state.exposure
        hue = state.hue
        temperature = state.temperature
        sharpness = state.sharpness
        luminance = state.luminance
        fade = state.fade
    }

    fun getBrightness() = brightness
    fun getContrast() = contrast
    fun getSaturation() = saturation
    fun getExposure() = exposure
    fun getHue() = hue
    fun getTemperature() = temperature
    fun getSharpness() = sharpness
    fun getLuminance() = luminance
    fun getFade() = fade
}