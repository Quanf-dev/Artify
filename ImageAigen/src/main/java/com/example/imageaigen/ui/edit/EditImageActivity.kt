package com.example.imageaigen.ui.edit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.common.base.BaseActivity
import com.example.imageaigen.databinding.ActivityEditImageBinding
import com.example.imageaigen.utils.NavigationUtils

class EditImageActivity : BaseActivity<ActivityEditImageBinding>() {
    private val viewModel: EditImageViewModel by viewModels()
    private var originalBitmap: Bitmap? = null
    private var editedBitmap: Bitmap? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                loadImageFromUri(uri)
            }
        }
    }

    override fun inflateBinding(): ActivityEditImageBinding {
        return ActivityEditImageBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
        
        // Kiểm tra xem có URI ảnh được truyền từ EditMainActivity không
        val imageUriString = intent.getStringExtra("image_uri")
        if (!imageUriString.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(imageUriString)
                loadImageFromUri(imageUri)
            } catch (e: Exception) {
                showErrorMessage("Error loading image: ${e.localizedMessage}")
            }
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.editImageButton.isEnabled = !isLoading && originalBitmap != null
            if (isLoading) showLoading() else hideLoading()
        }
        viewModel.imageEditResult.observe(this) { result ->
            result.bitmap?.let { bitmap ->
                editedBitmap = bitmap as Bitmap
                binding.originalImageView.setImageBitmap(bitmap)
                binding.editPromptEditText.setText("") // Clear prompt after successful edit
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

        binding.editImageButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                val prompt = binding.editPromptEditText.text.toString().trim()
                if (prompt.isNotEmpty()) {
                        viewModel.editImage(bitmap, prompt)
                } else {
                    showErrorMessage("Please enter an edit prompt")
                }
            }
        }

        binding.originalImageView.setOnClickListener {
            editedBitmap?.let { bitmap ->
                NavigationUtils.navigateToPreview(this, bitmap)
            } ?: run {
                originalBitmap?.let { bitmap ->
                    NavigationUtils.navigateToPreview(this, bitmap)
                }
            }
        }
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            editedBitmap = null // Reset edited bitmap when loading new image
            binding.originalImageView.setImageBitmap(originalBitmap)
            binding.originalImageView.visibility = View.VISIBLE
            binding.lnUpload.visibility = View.GONE
            binding.uploadContainer.background = null
            binding.editImageButton.isEnabled = true
        } catch (e: Exception) {
            showErrorMessage("Error loading image: ${e.localizedMessage}")
        }
    }
}