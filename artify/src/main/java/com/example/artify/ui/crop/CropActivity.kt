package com.example.artify.ui.crop

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.R
import com.example.artify.databinding.ActivityBlurBinding
import com.example.artify.databinding.ActivityCropBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID
import com.example.artify.ui.editbase.BaseEditActivity

class CropActivity : BaseEditActivity<ActivityCropBinding>() {

    private var sourceUri: Uri? = null

    private fun copyDrawableToCache(drawableResId: Int): Uri {
        val inputStream = resources.openRawResource(drawableResId)
        val file = File(cacheDir, "temp_crop_image.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return Uri.fromFile(file)
    }

    override fun inflateBinding(): ActivityCropBinding {
        return ActivityCropBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nhận ảnh đầu vào đồng bộ
        getInputBitmap(
            onBitmapReady = { bitmap ->
                // Lưu bitmap ra file tạm, lấy Uri rồi truyền cho UCrop
                val file = File(cacheDir, "temp_crop_image.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                sourceUri = Uri.fromFile(file)
                startUCrop()
            },
            onError = {
                sourceUri = copyDrawableToCache(R.drawable.img_animegen)
                startUCrop()
            }
        )
        finish()
    }

    private fun startUCrop() {
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            setResult(RESULT_OK, Intent().apply {
                putExtra("edited_image_path", resultUri?.path)
            })
            finish()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }
} 