package com.example.imageaigen.ui.removebg

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
import com.example.imageaigen.databinding.ActivityRemoveBackgroundBinding
import com.example.imageaigen.utils.NavigationUtils

class RemoveBackgroundActivity : BaseActivity<ActivityRemoveBackgroundBinding>() {
    private val viewModel: RemoveBackgroundViewModel by viewModels()
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

    override fun inflateBinding(): ActivityRemoveBackgroundBinding {
        return ActivityRemoveBackgroundBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.removeBgButton.isEnabled = !isLoading && originalBitmap != null
            if (isLoading) showLoading() else hideLoading()
        }
        viewModel.resultBitmap.observe(this) { bitmap ->
            bitmap?.let {
                NavigationUtils.navigateToPreview(this, it)
            } ?: run {
                showErrorMessage("Failed to remove background")
            }
        }
    }

    private fun setupClickListeners() {
        binding.uploadContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        binding.removeBgButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                viewModel.removeBackground(bitmap)
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
            binding.removeBgButton.isEnabled = true
        } catch (e: Exception) {
            showErrorMessage("Error loading image: ${e.localizedMessage}")
        }
    }
}