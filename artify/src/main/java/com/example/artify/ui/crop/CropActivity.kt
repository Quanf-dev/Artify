package com.example.artify.ui.crop

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.R
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

class CropActivity : AppCompatActivity() {

    private var sourceUri: Uri? = null

    private fun copyDrawableToCache(drawableResId: Int): Uri {
        val inputStream = resources.openRawResource(drawableResId)
        val file = File(cacheDir, "temp_crop_image.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return Uri.fromFile(file)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get source image URI from intent or use sample image
        sourceUri = intent.getParcelableExtra("image_uri") ?: copyDrawableToCache(R.drawable.img_animegen)

        // Create destination URI
        val destinationUri = Uri.fromFile(File(cacheDir, "${UUID.randomUUID()}.jpg"))

        // Configure UCrop options
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setShowCropGrid(true)
            setShowCropFrame(true)
            setCircleDimmedLayer(false)
            setStatusBarColor(getColor(R.color.black))
            setToolbarColor(getColor(R.color.black))
            setToolbarWidgetColor(getColor(R.color.white))
            setRootViewBackgroundColor(getColor(R.color.black))
        }

        // Start UCrop activity, allow user to freely choose aspect ratio
        UCrop.of(sourceUri!!, destinationUri)
            .withOptions(options)
            .start(this)

        // Close this activity since UCrop will handle everything
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            setResult(RESULT_OK, Intent().apply {
                putExtra("cropped_image_uri", resultUri.toString())
            })
            finish()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
} 