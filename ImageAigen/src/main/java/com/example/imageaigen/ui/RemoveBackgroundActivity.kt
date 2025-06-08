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
import androidx.lifecycle.ViewModelProvider
import com.example.imageaigen.databinding.ActivityRemoveBackgroundBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemoveBackgroundActivity : ComponentActivity() {
    private lateinit var binding: ActivityRemoveBackgroundBinding
    private lateinit var viewModel: RemoveBackgroundViewModel
    private var originalBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

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
        binding = ActivityRemoveBackgroundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[RemoveBackgroundViewModel::class.java]

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.removeBgButton.isEnabled = !isLoading && originalBitmap != null
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.resultBitmap.observe(this) { bitmap ->
            handleResult(bitmap)
        }
    }

    private fun setupClickListeners() {
        binding.selectImageButton.setOnClickListener {
            openImagePicker()
        }
        binding.removeBgButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                viewModel.removeBackground(bitmap)
            }
        }
        binding.saveResultButton.setOnClickListener {
            resultBitmap?.let { bitmap ->
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
            binding.removeBgButton.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleResult(bitmap: Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(this, "Failed to remove background", Toast.LENGTH_SHORT).show()
            return
        }
        resultBitmap = bitmap
        binding.resultImageView.setImageBitmap(bitmap)
        binding.resultImageCardView.visibility = View.VISIBLE
        binding.saveResultButton.visibility = View.VISIBLE
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "RemovedBG_${timestamp}.png"
        var outputStream: OutputStream? = null
        var saved = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                contentResolver.also { resolver ->
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = uri?.let { resolver.openOutputStream(it) }
                }
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    saved = true
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    saved = true
                }
            }
            if (saved) {
                Toast.makeText(this, "Result image saved to gallery", Toast.LENGTH_SHORT).show()
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