package com.example.artify.ui.blur

import android.graphics.Bitmap
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.SeekBar
import com.example.artify.R
import com.example.artify.databinding.ActivityBlurBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.artify.ui.editbase.animateImageIn
import java.io.File

class BlurActivity : BaseEditActivity<ActivityBlurBinding>() {

    private lateinit var toolbarBinding: ItemToolbarEditMainBinding
    private var originalBitmap: Bitmap? = null
    private var blurredBitmap: Bitmap? = null
    private var currentBlurLevel = 0f // Range: 0f - 25f

    override fun inflateBinding(): ActivityBlurBinding {
        return ActivityBlurBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        initViews()
        
        // Nhận ảnh đầu vào đồng bộ
        getInputBitmap(
            onBitmapReady = { bitmap ->
                originalBitmap = bitmap
                currentImageBitmap = bitmap
                binding.imageView.setImageBitmap(bitmap)
                binding.imageView.animateImageIn()
            },
            onError = {
            }
        )

        // Setup click listeners
        setupClickListeners()
    }

    private fun initViews() {
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root.findViewById<android.widget.LinearLayout>(R.id.tbBlur))

        // Setup SeekBar
        binding.seekBarBlur.max = 25
        binding.seekBarBlur.progress = 0
    }

    private fun setupClickListeners() {
        // SeekBar listener
        binding.seekBarBlur.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBlurLevel = progress.toFloat()
                binding.blurValueText.text = "$progress"

                // Don't apply blur continuously during dragging (performance)
                if (!fromUser || progress % 5 == 0) {
                    applyBlur(progress.toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Apply blur with the final value when user stops dragging
                applyBlur(currentBlurLevel)
            }
        })

        // Toolbar undo/redo buttons
        toolbarBinding.ivUndo.setOnClickListener {
            // Reset blur to previous level (for demo, just decrease by 5)
            val newLevel = (currentBlurLevel - 5).coerceAtLeast(0f)
            binding.seekBarBlur.progress = newLevel.toInt()
        }

        toolbarBinding.ivRedo.setOnClickListener {
            // Increase blur level (for demo, just increase by 5)
            val newLevel = (currentBlurLevel + 5).coerceAtMost(25f)
            binding.seekBarBlur.progress = newLevel.toInt()
        }
        
        // Add done button click listener
        toolbarBinding.ivDone.setOnClickListener {
            // Return the blurred image to EditMainActivity
            returnEditedImage(blurredBitmap ?: originalBitmap)
        }
    }

    private fun applyBlur(radius: Float) {
        if (originalBitmap == null) return

        // Make sure the radius is within valid range (0-25)
        val validRadius = radius.coerceIn(0f, 25f)

        if (validRadius > 0) {
            try {
                // Create a new bitmap to avoid modifying the original
                blurredBitmap = originalBitmap!!.copy(originalBitmap!!.config!!, true)

                // Apply RenderScript blur
                val renderScript = RenderScript.create(this)
                val input = Allocation.createFromBitmap(renderScript, blurredBitmap)
                val output = Allocation.createTyped(renderScript, input.type)
                val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

                script.setRadius(validRadius)
                script.setInput(input)
                script.forEach(output)
                output.copyTo(blurredBitmap)

                // Store the current blurred bitmap for potential return
                currentImageBitmap = blurredBitmap

                // Clean up RenderScript resources
                renderScript.destroy()

                // Update ImageView with blurred image
                binding.imageView.setImageBitmap(blurredBitmap)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // If radius is 0, show original image
            blurredBitmap = originalBitmap
            currentImageBitmap = originalBitmap
            binding.imageView.setImageBitmap(originalBitmap)
        }
    }
}