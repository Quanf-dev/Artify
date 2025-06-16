package com.example.artify.ui.crop

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import com.example.artify.constants.Constants
import com.example.common.R
import com.example.artify.databinding.ActivityCropBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID
import com.example.artify.ui.editbase.BaseEditActivity

class CropActivity : BaseEditActivity<ActivityCropBinding>() {

    private var sourceUri: Uri? = null

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
//                sourceUri = copyDrawableToCache(R.drawable.img_animegen)
//                startUCrop()
            }
        )
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
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                    // Load bitmap từ result URI và trả về bằng returnEditedImage
                    val croppedBitmap = BitmapFactory.decodeFile(resultUri.path)
                    returnEditedImage(croppedBitmap)
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            } else { // Bao gồm cả RESULT_CANCELED và UCrop.RESULT_ERROR
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }
} 