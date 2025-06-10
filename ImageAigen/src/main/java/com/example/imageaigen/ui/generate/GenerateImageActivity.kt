package com.example.imageaigen.ui.generate

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.common.base.BaseActivity
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.databinding.ActivityGenerateImageBinding
import com.example.imageaigen.utils.NavigationUtils

class GenerateImageActivity : BaseActivity<ActivityGenerateImageBinding>() {
    private val viewModel: GenerateImageViewModel by viewModels()

    override fun inflateBinding(): ActivityGenerateImageBinding {
        return ActivityGenerateImageBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.generateButton.isEnabled = !isLoading
            if (isLoading) showLoading() else hideLoading()
        }
        viewModel.images.observe(this) { bitmaps ->
            if (bitmaps.isNullOrEmpty()) {
                showErrorMessage("No images generated")
                return@observe
            }
            NavigationUtils.navigateToPreview(this, bitmaps)
        }
    }

    private fun setupClickListeners() {
        binding.generateButton.setOnClickListener {
            val prompt = binding.promptEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                viewModel.generateImage(prompt)
            } else {
                showErrorMessage("Please enter a prompt")
            }
        }
    }
}