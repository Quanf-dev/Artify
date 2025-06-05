package com.example.artify.ui.filter

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.example.artify.R
import com.example.artify.databinding.ActivityFilterBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.ui.views.ImageFilterView
import java.io.File

class FilterActivity : BaseEditActivity<ActivityFilterBinding>() {

    private lateinit var imageFilterView: ImageFilterView
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    override fun inflateBinding(): ActivityFilterBinding {
        return ActivityFilterBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        imageFilterView = binding.imageFilterView
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root)

        // Set up click listeners
        toolbarBinding.ivDone.setOnClickListener { 
            // Get the filtered image and return it to the EditMainActivity
            val filteredBitmap = imageFilterView.getCurrentBitmap()
            returnEditedImage(filteredBitmap)
        }
        
        toolbarBinding.ivRedo.setOnClickListener { imageFilterView.resetFilters() }

        // Check if we have an image path from intent
        val imagePath = intent.getStringExtra("image_path")
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    currentImageBitmap = bitmap
                    imageFilterView.setImageBitmap(bitmap)
                }
            }
        } else {
            // If no image path, try shared intent or load sample
            handleSharedImage(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedImage(intent)
    }

    private fun handleSharedImage(intent: Intent) {
        // Check if this is from a share intent
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            imageUri?.let {
                loadImageFromUri(it)
                return
            }
        }

    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                currentImageBitmap = bitmap
                imageFilterView.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
        }
    }


}