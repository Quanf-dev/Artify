package com.example.artify.ui.previewcamera

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.artify.ui.editMain.EditMainActivity
import com.example.camera.R
import com.example.camera.ui.preview.ImagePagerAdapter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreviewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var imageAdapter: ImagePagerAdapter
    private lateinit var imageCounter: TextView
    private var imagePaths: ArrayList<String> = ArrayList()

    // Xin quyền lưu trữ cho Android < 10
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            downloadCurrentImage()
        } else {
            Toast.makeText(
                this,
                "Storage permission is required to save images",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        viewPager = findViewById(R.id.view_pager)
        imageCounter = findViewById(R.id.image_counter)
        val backButton: ImageButton = findViewById(R.id.button_back)
        val downloadButton: ImageButton = findViewById(R.id.button_download)
        val editButton: ImageButton = findViewById(R.id.button_edit)
        val shareButton: ImageButton = findViewById(R.id.button_share)
        val deleteButton: ImageButton = findViewById(R.id.button_delete)

        // Get image paths from intent
        imagePaths = intent.getStringArrayListExtra(EXTRA_IMAGE_PATHS) ?: ArrayList()
        val currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0)

        Log.d(TAG, "Received image paths: ${imagePaths.size}, current position: $currentPosition")

        // If no paths are provided, use the single image path
        if (imagePaths.isEmpty()) {
            val singleImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
            if (singleImagePath != null) {
                Log.d(TAG, "Using single image path: $singleImagePath")
                imagePaths.add(singleImagePath)
            }
        }

        // Initialize adapter
        imageAdapter = ImagePagerAdapter(this, imagePaths)
        viewPager.adapter = imageAdapter

        // Set current position
        if (imagePaths.isNotEmpty() && currentPosition < imagePaths.size) {
            viewPager.setCurrentItem(currentPosition, false)
        }

        // Update counter text
        updateImageCounter(currentPosition)

        // Set page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateImageCounter(position)
            }
        })

        backButton.setOnClickListener {
            finish()
        }

        downloadButton.setOnClickListener {
            checkPermissionsAndDownload()
        }

        editButton.setOnClickListener {
            openImageEditor()
        }

        shareButton.setOnClickListener {
            shareCurrentImage()
        }

        deleteButton.setOnClickListener {
            confirmDeleteCurrentImage()
        }
    }

    private fun checkPermissionsAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) trở lên không cần xin quyền WRITE_EXTERNAL_STORAGE
            downloadCurrentImage()
        } else {
            // Android 9 (API 28) trở xuống cần xin quyền WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                downloadCurrentImage()
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun downloadCurrentImage() {
        val imagePath = getCurrentImagePath() ?: return
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "Artify_" + timeStamp

            var outputStream: OutputStream? = null
            var uri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (API 29) trở lên sử dụng MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$imageFileName.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Artify")
                }

                uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri == null) {
                    Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
                    return
                }
                outputStream = contentResolver.openOutputStream(uri)
            } else {
                // Android 9 (API 28) trở xuống sử dụng File API
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Artify")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val image = File(imagesDir, "$imageFileName.jpg")
                outputStream = FileOutputStream(image)
                uri = Uri.fromFile(image)
            }

            outputStream?.use { output ->
                FileInputStream(imageFile).use { input ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Image saved successfully to $uri")

        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to gallery", e)
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImageEditor() {
        val imagePath = getCurrentImagePath()
        if (imagePath == null) {
            Toast.makeText(this, "No image to edit", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(this, EditMainActivity::class.java)
            intent.putExtra("image_path", imagePath)
            Log.d(TAG, "Opening EditMainActivity with image path: $imagePath")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening editor: ${e.message}", e)
            Toast.makeText(this, "Failed to open editor: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImageCounter(position: Int) {
        if (imagePaths.isEmpty()) {
            imageCounter.text = "0/0"
        } else {
            imageCounter.text = "${position + 1}/${imagePaths.size}"
        }
    }

    private fun getCurrentImagePath(): String? {
        val currentPosition = viewPager.currentItem
        return if (currentPosition >= 0 && currentPosition < imagePaths.size) {
            imagePaths[currentPosition]
        } else {
            null
        }
    }

    private fun shareCurrentImage() {
        val imagePath = getCurrentImagePath() ?: return
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Log.d(TAG, "Sharing image: ${imageFile.absolutePath}, size: ${imageFile.length()} bytes")

            // Xác định authority từ AndroidManifest
            val authority = applicationContext.packageName + ".camera.fileprovider"
            Log.d(TAG, "Using FileProvider authority: $authority")

            // Tạo URI cho file
            val fileUri = FileProvider.getUriForFile(
                applicationContext, // Sử dụng applicationContext để tránh memory leak
                authority,
                imageFile
            )

            Log.d(TAG, "FileProvider URI created: $fileUri")

            // Tạo intent chia sẻ
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Hiển thị chooser
            startActivity(Intent.createChooser(shareIntent, "Share image using"))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error sharing image: ${e.message}", e)
            Toast.makeText(this, "Error sharing image: FileProvider not configured correctly", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing image: ${e.message}", e)
            Toast.makeText(this, "Failed to share image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteCurrentImage() {
        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCurrentImage()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCurrentImage() {
        val currentPosition = viewPager.currentItem
        if (currentPosition < 0 || currentPosition >= imagePaths.size) return

        val imagePath = imagePaths[currentPosition]
        val imageFile = File(imagePath)

        if (imageFile.exists() && imageFile.delete()) {
            // Remove from our list
            imagePaths.removeAt(currentPosition)

            if (imagePaths.isEmpty()) {
                // If no images left, go back
                finish()
            } else {
                // Update adapter and view pager
                imageAdapter = ImagePagerAdapter(this, imagePaths)
                viewPager.adapter = imageAdapter

                // Set position (stay at same position or go to previous if we deleted the last item)
                val newPosition = if (currentPosition >= imagePaths.size) {
                    imagePaths.size - 1
                } else {
                    currentPosition
                }
                viewPager.setCurrentItem(newPosition, false)
                updateImageCounter(newPosition)
            }
        } else {
            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // We don't delete the files here anymore since we want to keep them for browsing
    }

    companion object {
        private const val TAG = "PreviewActivity"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_IMAGE_PATHS = "extra_image_paths"
        const val EXTRA_CURRENT_POSITION = "extra_current_position"
    }
}