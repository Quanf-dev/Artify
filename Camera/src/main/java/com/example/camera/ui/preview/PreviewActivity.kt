package com.example.camera.ui.preview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.camera.R
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var imageAdapter: ImagePagerAdapter
    private lateinit var imageCounter: TextView
    private var imagePaths: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        viewPager = findViewById(R.id.view_pager)
        imageCounter = findViewById(R.id.image_counter)
        val backButton: ImageButton = findViewById(R.id.button_back)
        val shareButton: ImageButton = findViewById(R.id.button_share)
        val deleteButton: ImageButton = findViewById(R.id.button_delete)

        // Get image paths from intent
        imagePaths = intent.getStringArrayListExtra(EXTRA_IMAGE_PATHS) ?: ArrayList()
        val currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0)
        

        // If no paths are provided, use the single image path
        if (imagePaths.isEmpty()) {
            val singleImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
            if (singleImagePath != null) {
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
        
        shareButton.setOnClickListener {
            shareCurrentImage()
        }
        
        deleteButton.setOnClickListener {
            confirmDeleteCurrentImage()
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
            Log.d("PreviewActivity", "Sharing image: ${imageFile.absolutePath}, size: ${imageFile.length()} bytes")
            
            // Xác định authority từ AndroidManifest
            val authority = applicationContext.packageName + ".camera.fileprovider"
            Log.d("PreviewActivity", "Using FileProvider authority: $authority")
            
            // Tạo URI cho file
            val fileUri = FileProvider.getUriForFile(
                applicationContext, // Sử dụng applicationContext để tránh memory leak
                authority,
                imageFile
            )
            
            Log.d("PreviewActivity", "FileProvider URI created: $fileUri")
            
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
            Log.e("PreviewActivity", "Error sharing image: ${e.message}", e)
            Toast.makeText(this, "Error sharing image: FileProvider not configured correctly", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("PreviewActivity", "Error sharing image: ${e.message}", e)
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
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_IMAGE_PATHS = "extra_image_paths"
        const val EXTRA_CURRENT_POSITION = "extra_current_position"
    }
} 