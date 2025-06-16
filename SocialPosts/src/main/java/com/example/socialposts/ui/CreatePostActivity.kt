package com.example.socialposts.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.common.base.BaseActivity
import com.example.socialposts.databinding.ActivityCreatePostBinding
import com.example.socialposts.viewmodel.PostViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreatePostActivity : BaseActivity<ActivityCreatePostBinding>() {

    private val viewModel: PostViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var isProcessingPost = false

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivPostImage.setImageURI(uri)
                updatePostButtonState()
            }
        }
    }

    override fun inflateBinding(): ActivityCreatePostBinding {
        return ActivityCreatePostBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupListeners()
        observeViewModel()
        
        // Kiểm tra xem có URI ảnh được truyền từ EditMainActivity không
        val imageUriString = intent.getStringExtra("selected_image_uri")
        if (!imageUriString.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(imageUriString)
                selectedImageUri = imageUri
                binding.ivPostImage.setImageURI(imageUri)
                updatePostButtonState()
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        binding.btnPost.setOnClickListener {
            if (!isProcessingPost) {
                createPost()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        isProcessingPost = isLoading
                        if (isLoading) {
                            showLoading()
                        } else {
                            hideLoading()
                        }
                        binding.btnPost.isEnabled = !isLoading && selectedImageUri != null
                    }
                }

                launch {
                    viewModel.errorMessage.collect { errorMessage ->
                        errorMessage?.let {
                            showError(it)
                            viewModel.clearError()
                        }
                    }
                }

                launch {
                    viewModel.postCreationSuccess.collect { success ->
                        if (success) {
                            showSuccessMessage("Post created successfully!")
                            viewModel.resetPostCreationState()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    private fun createPost() {
        val caption = binding.etCaption.text.toString().trim()
        val finalCaption = if (caption.isEmpty()) "I create it easily by artify" else caption
        
        selectedImageUri?.let { uri ->
            viewModel.createPost(uri, finalCaption)
        }
    }

    private fun updatePostButtonState() {
        binding.btnPost.isEnabled = selectedImageUri != null && !isProcessingPost
    }
} 