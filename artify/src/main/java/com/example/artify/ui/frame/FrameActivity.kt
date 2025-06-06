package com.example.artify.ui.frame

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
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.artify.R
import com.example.artify.databinding.ActivityFrameBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.ui.views.ImageFrameView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FrameActivity : BaseEditActivity<ActivityFrameBinding>() {

    private lateinit var imageFrameView: ImageFrameView
    private lateinit var buttonLoadImage: Button
    private lateinit var buttonCameraImage: Button
    private lateinit var buttonSave: ImageButton
    private lateinit var buttonReset: ImageButton

    private var tempCameraUri: Uri? = null

    override fun inflateBinding(): ActivityFrameBinding {
        return ActivityFrameBinding.inflate(layoutInflater)
    }






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        imageFrameView = binding.imageFrameView
        buttonLoadImage = binding.buttonLoadImage
        buttonCameraImage = binding.buttonCameraImage
//        buttonSave = binding.buttonSave
//        buttonReset = binding.buttonReset

        // Set up click listeners

        buttonReset.setOnClickListener { imageFrameView.resetFrame() }

        // Check if the activity was launched from a share intent
        handleSharedImage(intent)
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
                loadBitmapFromUri(it)
                return
            }
        }

        // If no shared image, load a sample image
//        loadSampleImage()
    }

//    override fun loadImageFromUri(uri: Uri) {
//        try {
//            val inputStream = contentResolver.openInputStream(uri)
//            val bitmap = BitmapFactory.decodeStream(inputStream)
//            if (bitmap != null) {
//                imageFrameView.setImageBitmap(bitmap)
//            } else {
//                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
//        }
//    }

//    private fun loadSampleImage() {
//        try {
//            // Load a sample image from resources
//            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_animegen)
//            if (bitmap != null) {
//                imageFrameView.setImageBitmap(bitmap)
//            } else {
//                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
//        }
//    }



}