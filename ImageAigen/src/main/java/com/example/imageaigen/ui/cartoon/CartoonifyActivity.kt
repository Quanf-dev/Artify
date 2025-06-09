package com.example.imageaigen.ui.cartoon

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
import com.example.imageaigen.databinding.ActivityCartoonifyBinding
import com.example.imageaigen.utils.NavigationUtils

class CartoonifyActivity : BaseActivity<ActivityCartoonifyBinding>() {
    private val viewModel: CartoonifyViewModel by viewModels()
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

    override fun inflateBinding(): ActivityCartoonifyBinding {
        return ActivityCartoonifyBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.cartoonifyButton.isEnabled = !isLoading && originalBitmap != null
            if (isLoading) showLoading() else hideLoading()
        }
        viewModel.result.observe(this) { result ->
            if (result.isError) {
                showErrorMessage("Error: ${result.errorMessage}")
                return@observe
            }
            result.bitmap?.let { bitmap ->
                NavigationUtils.navigateToPreview(this, bitmap)
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

        binding.cartoonifyButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                viewModel.cartoonifyImage(bitmap)
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
            binding.cartoonifyButton.isEnabled = true
        } catch (e: Exception) {
            showErrorMessage("Error loading image: ${e.localizedMessage}")
        }
    }
} 