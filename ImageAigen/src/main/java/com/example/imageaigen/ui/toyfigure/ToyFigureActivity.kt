package com.example.imageaigen.ui.toyfigure

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import android.view.View
import com.example.common.base.BaseActivity
import com.example.imageaigen.databinding.ActivityToyFigureBinding
import com.example.imageaigen.utils.NavigationUtils

class ToyFigureActivity : BaseActivity<ActivityToyFigureBinding>() {
    private val viewModel: ToyFigureViewModel by viewModels()
    private var originalBitmap: Bitmap? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                loadImageFromUri(uri)
            }
        }
    }

    override fun inflateBinding(): ActivityToyFigureBinding {
        return ActivityToyFigureBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.generateToyButton.isEnabled = !isLoading && originalBitmap != null
            if (isLoading) showLoading() else hideLoading()
        }

        viewModel.images.observe(this) { images ->
            if (images.isEmpty()) {
                showErrorMessage("No toy figures generated")
                return@observe
            }
            NavigationUtils.navigateToPreview(this, images)
        }
    }

    private fun setupClickListeners() {
        binding.uploadContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        binding.generateToyButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val occupation = binding.occupationEditText.text.toString().trim()
            val accessories = binding.accessoriesEditText.text.toString().trim()
            val clothing = binding.clothingEditText.text.toString().trim()
            
            if (name.isEmpty() || occupation.isEmpty() || accessories.isEmpty() || clothing.isEmpty()) {
                showErrorMessage("Please fill all fields")
                return@setOnClickListener
            }
            
            if (originalBitmap == null) {
                showErrorMessage("Please select an image")
                return@setOnClickListener
            }
            
            val prompt = "Create a cartoon-style full-figure toy model of the character in the image below, displayed inside colorful plastic blister packaging. " +
                    "At the top of the toy box, the name of the toy is written as '$name', and underneath it, their job title '$occupation' is shown — both on two separate lines in bold, fun fonts." +
                    "The character stands cheerfully in the center of the package, with a big, confident smile. " +
                    "Surrounding them are cute, oversized accessories representing their profession: $accessories, each drawn in a playful, exaggerated style. " +
                    "The character is wearing $clothing, designed with bold lines, bright colors, and a youthful cartoon flair." +
                    "The blister packaging itself features a modern, vibrant design — full of energetic patterns and colors that reflect the dynamic and creative work lifestyle of today's youth." +
                    "Photorealistic rendering, studio lightning, clear focus on the packaging and figure."

            originalBitmap?.let {
                viewModel.generateToyFigure(it, prompt)
            }
        }
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            binding.originalImageView.setImageBitmap(originalBitmap)
            binding.originalImageView.visibility = View.VISIBLE
            binding.lnUpload.visibility = View.GONE
            binding.uploadContainer.background = null
            binding.generateToyButton.isEnabled = true
        } catch (e: Exception) {
            showErrorMessage("Error loading image: ${e.localizedMessage}")
        }
    }
} 