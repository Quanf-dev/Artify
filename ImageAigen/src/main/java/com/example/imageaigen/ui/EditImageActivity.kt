package com.example.imageaigen.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.databinding.ActivityEditImageBinding
import com.example.imageaigen.ui.edit.EditImageViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditImageActivity : ComponentActivity() {
    private lateinit var binding: ActivityEditImageBinding
    private val viewModel: EditImageViewModel by viewModels()
    private var originalBitmap: Bitmap? = null
    private var editedBitmap: Bitmap? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                loadImageFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.editImageButton.isEnabled = !isLoading && originalBitmap != null
        }
        viewModel.imageEditResult.observe(this) { result ->
            handleEditResult(result)
        }
    }

    private fun setupClickListeners() {
//        binding.selectImageButton.setOnClickListener {
//            openImagePicker()
//        }
        binding.editImageButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                val prompt = binding.editPromptEditText.text.toString().trim()
                if (prompt.isNotEmpty()) {
                    viewModel.editImage(bitmap, prompt)
                } else {
                    Toast.makeText(this, "Please enter an edit prompt", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.saveEditedImageButton.setOnClickListener {
            editedBitmap?.let { bitmap ->
                saveImageToGallery(bitmap)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            binding.originalImageView.setImageBitmap(originalBitmap)
            binding.editImageButton.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleEditResult(result: GeminiResponse) {
        if (result.isError) {
            Toast.makeText(this, "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
            return
        }
        result.bitmap?.let { bitmap ->
            editedBitmap = bitmap
            binding.editedImageView.setImageBitmap(bitmap)
            binding.editedImageCardView.visibility = View.VISIBLE
            binding.saveEditedImageButton.visibility = View.VISIBLE
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "GeminiEdit_${timestamp}.jpg"
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
                Toast.makeText(this, "Edited image saved to gallery", Toast.LENGTH_SHORT).show()
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