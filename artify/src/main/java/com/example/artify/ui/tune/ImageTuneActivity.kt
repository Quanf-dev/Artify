package com.example.artify.ui.tune

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.example.artify.R
import com.example.artify.databinding.ActivityImageTuneBinding
import com.example.artify.databinding.ItemBottomTuneBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.ui.views.ImageAdjustmentView
import java.io.File

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
        
        // Load image from intent
        val imagePath = intent.getStringExtra("image_path")
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    currentImageBitmap = bitmap
                    imageAdjustmentView.setImageBitmap(bitmap)
                } else {
                    loadSampleImage()
                }
            } else {
                loadSampleImage()
            }
        } else {
            loadSampleImage()
        }
        
        // Setup click listeners
        setupClickListeners()
    }

    private fun initViews() {
        imageAdjustmentView = binding.imageAdjustmentView
        bottomTuneBinding = ItemBottomTuneBinding.bind(binding.bottomTuneMenu.root)
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root.findViewById(R.id.tbMain))
    }

    private fun loadSampleImage() {
        // Load img_animegen.png from resources
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_animegen)
        currentImageBitmap = bitmap
        imageAdjustmentView.setImageBitmap(bitmap)
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
            val editedBitmap = imageAdjustmentView.getEditedBitmap()
            // Return it to the EditMainActivity
            returnEditedImage(editedBitmap)
        }
    }
}