package com.example.imageaigen.ui.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.common.base.BaseActivity
import com.example.imageaigen.databinding.ActivityPreviewImageBinding
import com.example.imageaigen.utils.NavigationUtils
import java.io.File
import java.util.UUID

class PreviewImageActivity : BaseActivity<ActivityPreviewImageBinding>() {
    private val viewModel: PreviewImageViewModel by viewModels()
    private lateinit var adapter: PreviewImageAdapter
    private var currentImageIndex = 0

    override fun inflateBinding(): ActivityPreviewImageBinding {
        return ActivityPreviewImageBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRecyclerView()
        setupObservers()
        loadImagesFromIntent()
    }

    private fun setupRecyclerView() {
        adapter = PreviewImageAdapter(
            onImageClick = { position ->
                currentImageIndex = position
            },
            onDownloadClick = { position ->
                viewModel.images.value?.getOrNull(position)?.let { bitmap ->
                    NavigationUtils.viewImage(this, bitmap)
                }
            },
            onShareClick = { position ->
                viewModel.images.value?.getOrNull(position)?.let { bitmap ->
                    NavigationUtils.shareImage(this, bitmap)
                }
            },
            onEditClick = { position ->
                viewModel.images.value?.getOrNull(position)?.let { bitmap ->
                    NavigationUtils.navigateToEdit(this, bitmap)
                }
            }
        )
        binding.imagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PreviewImageActivity)
            adapter = this@PreviewImageActivity.adapter
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }
    }

    private fun loadImagesFromIntent() {
        // Try to get single image URI first
        val singleImageUri = intent.getParcelableExtra<Uri>("image_uri")
        if (singleImageUri != null) {
            loadSingleImage(singleImageUri)
            return
        }

        // If no single image, try to get list of URIs
        val imageUris = intent.getParcelableArrayListExtra<Uri>("image_uris")
        if (!imageUris.isNullOrEmpty()) {
            loadMultipleImages(imageUris)
            return
        }
        
        showErrorMessage("No images to preview")
        finish()
    }

    private fun loadSingleImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val imageItem = PreviewImageItem(UUID.randomUUID().toString(), bitmap)
            adapter.submitList(listOf(imageItem))
            viewModel.setImages(listOf(bitmap))
        } catch (e: Exception) {
            showErrorMessage("Error loading image: ${e.localizedMessage}")
            finish()
        }
    }

    private fun loadMultipleImages(uris: ArrayList<Uri>) {
        try {
            val images = uris.map { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                PreviewImageItem(UUID.randomUUID().toString(), bitmap)
            }
            adapter.submitList(images)
            viewModel.setImages(images.map { it.bitmap })
        } catch (e: Exception) {
            showErrorMessage("Error loading images: ${e.localizedMessage}")
            finish()
        }
    }
}