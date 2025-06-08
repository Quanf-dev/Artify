package com.example.imageaigen.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.databinding.ActivityGenerateImageBinding
import com.example.imageaigen.ui.generate.GenerateImageViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerateImageActivity : ComponentActivity() {
    private lateinit var binding: ActivityGenerateImageBinding
    private val viewModel: GenerateImageViewModel by viewModels()
    private var generatedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.generateButton.isEnabled = !isLoading
        }
        viewModel.imageGenerationResult.observe(this) { result ->
            handleGenerationResult(result)
        }
    }

    private fun setupClickListeners() {
        binding.generateButton.setOnClickListener {
            val prompt = binding.promptEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                viewModel.generateImage(prompt)
            } else {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }
        binding.saveImageButton.setOnClickListener {
            generatedBitmap?.let { bitmap ->
                saveImageToGallery(bitmap)
            }
        }
        binding.editImageButton.setOnClickListener {
            generatedBitmap?.let { bitmap ->
                // Optionally: start EditImageActivity with this bitmap
                Toast.makeText(this, "Implement navigation to EditImageActivity if needed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGenerationResult(result: GeminiResponse) {
        if (result.isError) {
            Toast.makeText(this, "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
            return
        }
        result.bitmap?.let { bitmap ->
            generatedBitmap = bitmap
            binding.generatedImageView.setImageBitmap(bitmap)
            binding.generatedImageView.visibility = View.VISIBLE
            binding.actionButtonsLayout.visibility = View.VISIBLE
        }
        result.text?.let { text ->
            if (text.isNotEmpty()) {
                binding.responseTextView.text = text
                binding.responseCardView.visibility = View.VISIBLE
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "Gemini_${timestamp}.jpg"
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
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            outputStream?.close()
        }
    }
} 