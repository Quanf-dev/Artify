package com.example.artify.ui.sticker

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.artify.R
import com.example.artify.databinding.ActivityStickerBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.ui.views.ImageStickerView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StickerActivity : BaseEditActivity<ActivityStickerBinding>() {

    private lateinit var imageStickerView: ImageStickerView
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    override fun inflateBinding(): ActivityStickerBinding {
        return ActivityStickerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root.findViewById(R.id.tbSticker))
        imageStickerView = binding.imageStickerView
        binding.buttonClearStickers.setOnClickListener { imageStickerView.removeAllStickers() }
        toolbarBinding.ivDone.setOnClickListener {
            val editedBitmap = imageStickerView.getEditedBitmap()
            returnEditedImage(editedBitmap)
        }

        // Load image from intent if available
        val imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            setImageToViewFromFilePath(imagePath, {
                imageStickerView.setImageBitmap(it)
            })
        } else {
            // Check if the activity was launched from a share intent
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
                imageStickerView.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
        }
    }



}