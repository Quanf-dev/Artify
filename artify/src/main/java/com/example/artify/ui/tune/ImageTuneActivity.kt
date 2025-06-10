package com.example.artify.ui.tune

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import com.example.artify.R
import com.example.artify.databinding.ActivityImageTuneBinding
import com.example.artify.databinding.ItemBottomTuneBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.artify.ui.editbase.animateImageIn
import com.example.imageeditor.ui.views.ImageAdjustmentView

class ImageTuneActivity : BaseEditActivity<ActivityImageTuneBinding>() {

    // Views
    private lateinit var imageAdjustmentView: ImageAdjustmentView
    private lateinit var bottomTuneBinding: ItemBottomTuneBinding
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    override fun inflateBinding(): ActivityImageTuneBinding {
        return ActivityImageTuneBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize views
        initViews()
        
        // Ẩn bottom menu để chuẩn bị cho animation
        binding.bottomTuneMenu.root.visibility = View.INVISIBLE
        
        // Nhận ảnh đầu vào đồng bộ
        getInputBitmap(
            onBitmapReady = { bitmap ->
                currentImageBitmap = bitmap
                imageAdjustmentView.setImageBitmap(bitmap)
                imageAdjustmentView.animateImageIn()
                
                // Hiển thị bottom menu với animation sau khi ảnh đã load
                animateBottomBar(binding.bottomTuneMenu.root)
            },
            onError = {
                // Vẫn hiển thị bottom menu với animation
                animateBottomBar(binding.bottomTuneMenu.root)
            }
        )
        
        // Setup click listeners
        setupClickListeners()
    }

    private fun initViews() {
        imageAdjustmentView = binding.imageAdjustmentView
        bottomTuneBinding = ItemBottomTuneBinding.bind(binding.bottomTuneMenu.root)
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root.findViewById(R.id.tbMain))
    }


    private fun setupClickListeners() {
        bottomTuneBinding.llBrightness.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.BRIGHTNESS)
        }
        bottomTuneBinding.llContrast.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.CONTRAST)
        }
        bottomTuneBinding.llSaturation.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.SATURATION)
        }
        bottomTuneBinding.llExposure.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.EXPOSURE)
        }
        bottomTuneBinding.llHue.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.HUE)
        }
        bottomTuneBinding.llTemperture.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.TEMPERATURE)
        }
        bottomTuneBinding.llSharpness.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.SHARPNESS)
        }
        bottomTuneBinding.llLuminance.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.LUMINANCE)
        }
        bottomTuneBinding.llFade.setOnClickListener {
            imageAdjustmentView.setAdjustmentType(ImageAdjustmentView.AdjustmentType.FADE)
        }

        toolbarBinding.ivUndo.setOnClickListener {
            imageAdjustmentView.undo()
        }
        toolbarBinding.ivRedo.setOnClickListener {
            imageAdjustmentView.redo()
        }
        
        // Add done button handler
        toolbarBinding.ivDone.setOnClickListener {
            // Get the edited image bitmap
            val editedBitmap = imageAdjustmentView.getCurrentBitmap()
            // Return it to the EditMainActivity
            returnEditedImage(editedBitmap)
        }
    }
}