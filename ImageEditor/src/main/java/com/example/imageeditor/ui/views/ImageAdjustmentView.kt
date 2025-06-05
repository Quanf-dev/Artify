package com.example.imageeditor.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.example.imageeditor.R
import com.example.imageeditor.utils.ImageAdjustmentManager
import com.example.imageeditor.utils.AdjustmentState

/**
 * View that handles image adjustments like brightness, contrast, etc.
 */
class ImageAdjustmentView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Views
    private lateinit var imageView: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var labelView: TextView
    private lateinit var valueView: TextView
    
    // Image adjustment manager
    private val adjustmentManager = ImageAdjustmentManager()
    
    // Current adjustment type
    private var currentAdjustmentType = AdjustmentType.BRIGHTNESS
    
    // Listeners
    private var onImageChangedListener: ((Bitmap?) -> Unit)? = null

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_image_adjustment, this, true)
        
        // Get views
        imageView = findViewById(R.id.imageView)
        seekBar = findViewById(R.id.seekBar)
        labelView = findViewById(R.id.labelAdjustment)
        valueView = findViewById(R.id.valueAdjustment)
        
        // Setup seekbar listener
        setupSeekBarListener()
    }
    
    /**
     * Set the bitmap to be edited
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            adjustmentManager.setOriginalBitmap(bitmap)
            updateImageView()
            updateSeekBarRange()
            updateValue()
        }
    }
    
    /**
     * Set the current adjustment type
     */
    fun setAdjustmentType(type: AdjustmentType) {
        currentAdjustmentType = type
        updateSeekBarRange()
        updateLabel()
        updateValue()
    }
    
    /**
     * Reset all adjustments
     */
    fun resetAdjustments() {
        adjustmentManager.resetAdjustments()
        updateSeekBarRange()
        updateImageView()
        updateValue()
    }
    
    /**
     * Get the current edited bitmap
     */
    fun getCurrentBitmap(): Bitmap? {
        return adjustmentManager.getCurrentBitmap()
    }
    
    /**
     * Set a listener to be called when the image changes
     */
    fun setOnImageChangedListener(listener: (Bitmap?) -> Unit) {
        onImageChangedListener = listener
    }
    
    /**
     * Update the image view with current bitmap
     */
    private fun updateImageView() {
        val bitmap = adjustmentManager.getCurrentBitmap()
        imageView.setImageBitmap(bitmap)
        onImageChangedListener?.invoke(bitmap)
    }
    
    /**
     * Setup the seekbar listener
     */
    private fun setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    applyAdjustment(progress, pushToStack = false)
                    updateValue()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Lưu trạng thái khi người dùng dừng kéo
                applyAdjustment(seekBar?.progress ?: 0, pushToStack = true)
                updateValue()
            }
        })
    }
    
    /**
     * Apply adjustment based on seekbar progress
     */
    private fun applyAdjustment(progress: Int, pushToStack: Boolean) {
        val value = convertProgressToValue(progress)
        
        val bitmap = when (currentAdjustmentType) {
            AdjustmentType.BRIGHTNESS -> adjustmentManager.setBrightness(value, pushToStack)
            AdjustmentType.CONTRAST -> adjustmentManager.setContrast(value, pushToStack)
            AdjustmentType.SATURATION -> adjustmentManager.setSaturation(value, pushToStack)
            AdjustmentType.EXPOSURE -> adjustmentManager.setExposure(value, pushToStack)
            AdjustmentType.HUE -> adjustmentManager.setHue(value, pushToStack)
            AdjustmentType.TEMPERATURE -> adjustmentManager.setTemperature(value, pushToStack)
            AdjustmentType.SHARPNESS -> adjustmentManager.setSharpness(value, pushToStack)
            AdjustmentType.LUMINANCE -> adjustmentManager.setLuminance(value, pushToStack)
            AdjustmentType.FADE -> adjustmentManager.setFade(value, pushToStack)
        }
        
        imageView.setImageBitmap(bitmap)
        onImageChangedListener?.invoke(bitmap)
    }
    
    /**
     * Update the seekbar range based on current adjustment type
     */
    private fun updateSeekBarRange() {
        val (min, max, current) = when (currentAdjustmentType) {
            AdjustmentType.BRIGHTNESS -> Triple(0, 200, valueToProgress(adjustmentManager, AdjustmentType.BRIGHTNESS))
            AdjustmentType.CONTRAST -> Triple(0, 100, valueToProgress(adjustmentManager, AdjustmentType.CONTRAST))
            AdjustmentType.SATURATION -> Triple(0, 200, valueToProgress(adjustmentManager, AdjustmentType.SATURATION))
            AdjustmentType.EXPOSURE -> Triple(0, 100, valueToProgress(adjustmentManager, AdjustmentType.EXPOSURE))
            AdjustmentType.HUE -> Triple(0, 360, valueToProgress(adjustmentManager, AdjustmentType.HUE))
            AdjustmentType.TEMPERATURE -> Triple(0, 200, valueToProgress(adjustmentManager, AdjustmentType.TEMPERATURE))
            AdjustmentType.SHARPNESS -> Triple(0, 100, valueToProgress(adjustmentManager, AdjustmentType.SHARPNESS))
            AdjustmentType.LUMINANCE -> Triple(0, 100, valueToProgress(adjustmentManager, AdjustmentType.LUMINANCE))
            AdjustmentType.FADE -> Triple(0, 100, valueToProgress(adjustmentManager, AdjustmentType.FADE))
        }
        
        seekBar.max = max
        seekBar.progress = current
    }
    
    /**
     * Update the label based on current adjustment type
     */
    private fun updateLabel() {
        val labelText = when (currentAdjustmentType) {
            AdjustmentType.BRIGHTNESS -> "Brightness"
            AdjustmentType.CONTRAST -> "Contrast"
            AdjustmentType.SATURATION -> "Saturation"
            AdjustmentType.EXPOSURE -> "Exposure"
            AdjustmentType.HUE -> "Hue"
            AdjustmentType.TEMPERATURE -> "Temperature"
            AdjustmentType.SHARPNESS -> "Sharpness"
            AdjustmentType.LUMINANCE -> "Luminance"
            AdjustmentType.FADE -> "Fade"
        }
        
        labelView.text = labelText
    }
    
    /**
     * Update the value display based on current seekbar progress
     */
    private fun updateValue() {
        val value = convertProgressToValue(seekBar.progress)
        
        val displayValue = when (currentAdjustmentType) {
            AdjustmentType.BRIGHTNESS -> "${value.toInt()}"
            AdjustmentType.CONTRAST -> String.format("%.2f", value)
            AdjustmentType.SATURATION -> String.format("%.2f", value)
            AdjustmentType.EXPOSURE -> String.format("%.2f", value)
            AdjustmentType.HUE -> "${value.toInt()}°"
            AdjustmentType.TEMPERATURE -> "${value.toInt()}"
            AdjustmentType.SHARPNESS -> "${value.toInt()}%"
            AdjustmentType.LUMINANCE -> String.format("%.2f", value)
            AdjustmentType.FADE -> "${value.toInt()}%"
        }
        
        valueView.text = displayValue
    }
    
    /**
     * Convert seekbar progress to the appropriate value for each adjustment type
     */
    private fun convertProgressToValue(progress: Int): Float {
        return when (currentAdjustmentType) {
            AdjustmentType.BRIGHTNESS -> progress - 100f
            AdjustmentType.CONTRAST -> 0.5f + (progress / 100f)
            AdjustmentType.SATURATION -> progress / 100f * 2f
            AdjustmentType.EXPOSURE -> 0.5f + (progress / 100f)
            AdjustmentType.HUE -> progress - 180f
            AdjustmentType.TEMPERATURE -> progress - 100f
            AdjustmentType.SHARPNESS -> progress.toFloat()
            AdjustmentType.LUMINANCE -> 0.5f + (progress / 100f)
            AdjustmentType.FADE -> progress.toFloat()
        }
    }
    
    // Chuyển giá trị hiện tại về progress cho seekbar
    private fun valueToProgress(manager: ImageAdjustmentManager, type: AdjustmentType): Int {
        return when (type) {
            AdjustmentType.BRIGHTNESS -> (manager.getBrightness() + 100).toInt()
            AdjustmentType.CONTRAST -> ((manager.getContrast() - 0.5f) * 100).toInt()
            AdjustmentType.SATURATION -> ((manager.getSaturation() / 2f) * 100).toInt()
            AdjustmentType.EXPOSURE -> ((manager.getExposure() - 0.5f) * 100).toInt()
            AdjustmentType.HUE -> (manager.getHue() + 180).toInt()
            AdjustmentType.TEMPERATURE -> (manager.getTemperature() + 100).toInt()
            AdjustmentType.SHARPNESS -> manager.getSharpness().toInt()
            AdjustmentType.LUMINANCE -> ((manager.getLuminance() - 0.5f) * 100).toInt()
            AdjustmentType.FADE -> manager.getFade().toInt()
        }
    }
    
    // Thêm getter cho các tham số
    fun getBrightness() = adjustmentManager.getBrightness()
    fun getContrast() = adjustmentManager.getContrast()
    fun getSaturation() = adjustmentManager.getSaturation()
    fun getExposure() = adjustmentManager.getExposure()
    fun getHue() = adjustmentManager.getHue()
    fun getTemperature() = adjustmentManager.getTemperature()
    fun getSharpness() = adjustmentManager.getSharpness()
    fun getLuminance() = adjustmentManager.getLuminance()
    fun getFade() = adjustmentManager.getFade()

    // Undo/Redo public
    fun undo() {
        val state = adjustmentManager.undo()
        if (state != null) {
            imageView.setImageBitmap(state.bitmap)
            // Cập nhật lại seekbar theo trạng thái hiện tại
            updateSeekBarRange()
            updateValue()
        }
    }
    fun redo() {
        val state = adjustmentManager.redo()
        if (state != null) {
            imageView.setImageBitmap(state.bitmap)
            updateSeekBarRange()
            updateValue()
        }
    }
    
    /**
     * Enum for different adjustment types
     */
    enum class AdjustmentType {
        BRIGHTNESS,
        CONTRAST,
        SATURATION,
        EXPOSURE,
        HUE,
        TEMPERATURE,
        SHARPNESS,
        LUMINANCE,
        FADE
    }

} 