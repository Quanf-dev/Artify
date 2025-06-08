package com.example.imageaigen.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.databinding.ActivityAnimeGenBinding
import com.example.imageaigen.databinding.ItemGeneratedImageBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnimeGenActivity : ComponentActivity() {
    private lateinit var binding: ActivityAnimeGenBinding
    private lateinit var viewModel: AnimeGenViewModel
    private var generatedBitmaps: List<Bitmap> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimeGenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[AnimeGenViewModel::class.java]

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.generateButton.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.images.observe(this) { images ->
            handleImagesResult(images)
        }
    }

    private fun setupClickListeners() {
        binding.generateButton.setOnClickListener {
            val prompt = binding.promptEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                clearGeneratedImages()
                viewModel.generateAnimeImages(prompt)
            } else {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleImagesResult(images: List<Bitmap>) {
        generatedBitmaps = images
        binding.generatedImagesContainer.removeAllViews()
        if (images.isEmpty()) {
            Toast.makeText(this, "No images generated", Toast.LENGTH_SHORT).show()
            return
        }
        images.forEachIndexed { index, bitmap ->
            val itemBinding = ItemGeneratedImageBinding.inflate(LayoutInflater.from(this), binding.generatedImagesContainer, false)
            itemBinding.generatedImageView.setImageBitmap(bitmap)
            itemBinding.saveImageButton.setOnClickListener {
                saveImageToGallery(bitmap, index)
            }
            binding.generatedImagesContainer.addView(itemBinding.root)
        }
    }

    private fun clearGeneratedImages() {
        binding.generatedImagesContainer.removeAllViews()
        generatedBitmaps = emptyList()
    }

    private fun saveImageToGallery(bitmap: Bitmap, index: Int) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "AnimeGen_${timestamp}_$index.jpg"
        var outputStream: OutputStream? = null
        var saved = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                contentResolver.also { resolver ->
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = uri?.let { resolver.openOutputStream(it) }
                }
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                    saved = true
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                    saved = true
                }
            }

            if (saved) {
                Toast.makeText(this, "Image $index saved to gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save image $index", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            outputStream?.close()
        }
    }
} 