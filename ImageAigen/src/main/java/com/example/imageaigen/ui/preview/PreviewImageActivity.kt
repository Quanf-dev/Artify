package com.example.imageaigen.ui.preview

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.common.base.BaseActivity
import com.example.imageaigen.databinding.ActivityPreviewImageBinding
import com.example.imageaigen.utils.NavigationUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PreviewImageActivity : BaseActivity<ActivityPreviewImageBinding>() {
    private val viewModel: PreviewImageViewModel by viewModels()
    private lateinit var adapter: PreviewImageAdapter
    private var currentImageIndex = 0
    private var pendingDownloadBitmap: Bitmap? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingDownloadBitmap?.let { bitmap ->
                saveBitmapToGallery(bitmap)
                pendingDownloadBitmap = null
            }
        } else {
            showErrorMessage("Storage permission is required to save images")
        }
    }

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
                    downloadImage(bitmap)
                }
            },
            onShareClick = { position ->
                viewModel.images.value?.getOrNull(position)?.let { bitmap ->
                    shareImageWithWatermark(bitmap)
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

    private fun downloadImage(bitmap: Bitmap) {
        if (hasStoragePermission()) {
            saveBitmapToGallery(bitmap)
        } else {
            pendingDownloadBitmap = bitmap
            requestStoragePermission()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // No permission needed for scoped storage
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed for Android 10+
            pendingDownloadBitmap?.let { bitmap ->
                saveBitmapToGallery(bitmap)
                pendingDownloadBitmap = null
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "Artify_$timestamp.jpg"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Artify")
                }
                
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { imageUri ->
                    contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        showSuccessMessage("Image saved to gallery")
                    }
                } ?: run {
                    showErrorMessage("Failed to save image")
                }
            } else {
                // Use legacy storage for older Android versions
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val artifyDir = File(picturesDir, "Artify")
                if (!artifyDir.exists()) {
                    artifyDir.mkdirs()
                }
                
                val imageFile = File(artifyDir, filename)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    showSuccessMessage("Image saved to ${imageFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            showErrorMessage("Failed to save image: ${e.localizedMessage}")
        }
    }

    private fun shareImageWithWatermark(bitmap: Bitmap) {
        try {
            val watermarkedBitmap = addWatermarkToBitmap(bitmap, "artify")
            NavigationUtils.shareImage(this, watermarkedBitmap)
        } catch (e: Exception) {
            showErrorMessage("Failed to share image: ${e.localizedMessage}")
        }
    }

    private fun addWatermarkToBitmap(originalBitmap: Bitmap, watermarkText: String): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        // Create a new bitmap with the same dimensions
        val watermarkedBitmap = Bitmap.createBitmap(width, height, originalBitmap.config!!)
        val canvas = Canvas(watermarkedBitmap)
        
        // Draw the original bitmap
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        // Create paint for watermark text
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = calculateWatermarkTextSize(width, height)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK) // Add shadow for better visibility
        }
        
        // Calculate text position (bottom right corner with padding)
        val textWidth = paint.measureText(watermarkText)
        val textHeight = paint.textSize
        val padding = Math.min(width, height) * 0.03f // 3% of the smaller dimension
        
        val x = width - textWidth - padding
        val y = height - padding
        
        // Draw the watermark
        canvas.drawText(watermarkText, x, y, paint)
        
        return watermarkedBitmap
    }

    private fun calculateWatermarkTextSize(imageWidth: Int, imageHeight: Int): Float {
        val baseSize = Math.min(imageWidth, imageHeight) * 0.05f // 5% of the smaller dimension
        return Math.max(baseSize, 24f) // Minimum 24dp
    }

  
}