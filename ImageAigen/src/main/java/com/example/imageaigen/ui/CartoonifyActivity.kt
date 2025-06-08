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
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.databinding.ActivityCartoonifyBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CartoonifyActivity : ComponentActivity() {
    private lateinit var binding: ActivityCartoonifyBinding
    private lateinit var viewModel: CartoonifyViewModel
    private var originalBitmap: Bitmap? = null
    private var cartoonBitmap: Bitmap? = null

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
        binding = ActivityCartoonifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[CartoonifyViewModel::class.java]

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.cartoonifyButton.isEnabled = !isLoading && originalBitmap != null
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.result.observe(this) { result ->
            handleCartoonifyResult(result)
        }
    }

    private fun setupClickListeners() {
        binding.selectImageButton.setOnClickListener {
            openImagePicker()
        }

        binding.cartoonifyButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                viewModel.cartoonifyImage(bitmap)
            }
        }

        binding.saveCartoonImageButton.setOnClickListener {
            cartoonBitmap?.let { bitmap ->
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
            binding.cartoonifyButton.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCartoonifyResult(result: GeminiResponse) {
        if (result.isError) {
            Toast.makeText(this, "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
            return
        }

        result.bitmap?.let { bitmap ->
            cartoonBitmap = bitmap
            binding.cartoonImageView.setImageBitmap(bitmap)
            binding.cartoonImageCardView.visibility = View.VISIBLE
            binding.saveCartoonImageButton.visibility = View.VISIBLE
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "Cartoon_${timestamp}.jpg"
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
                Toast.makeText(this, "Cartoon image saved to gallery", Toast.LENGTH_SHORT).show()
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