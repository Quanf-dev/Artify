package com.example.artify.ui.editbase

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.artify.databinding.ActivityBaseEditBinding
import com.example.artify.ui.blur.BlurActivity
import com.example.artify.ui.crop.CropActivity
import com.example.artify.ui.filter.FilterActivity
import com.example.artify.ui.frame.FrameActivity
import com.example.artify.ui.paint.PaintActivity
import com.example.artify.ui.sticker.StickerActivity
import com.example.artify.ui.tune.ImageTuneActivity
import com.example.artify.utils.navigate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

abstract class BaseEditActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var baseBinding: ActivityBaseEditBinding
    protected lateinit var binding: VB
    protected var currentImageBitmap: Bitmap? = null

    abstract fun inflateBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseBinding = ActivityBaseEditBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = inflateBinding()
        baseBinding.contentContainerEdit.addView(binding.root)
    }

    protected fun navigateToPaint() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, PaintActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToText() {
        // Navigate to text screen with current image bitmap
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, StickerActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToCrop() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, CropActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToTune() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, ImageTuneActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToFilter() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, FilterActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToBlur() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, BlurActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToEmoji() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, StickerActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun returnEditedImage(bitmap: Bitmap?) {
        bitmap?.let {
            val tempFile = saveBitmapToTempFile(it)
            val intent = Intent()
            intent.putExtra("edited_image_path", tempFile?.absolutePath)
            setResult(Activity.RESULT_OK, intent)
            finish()
        } ?: run {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap?): File? {
        if (bitmap == null) return null
        
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            
            val file = File(cachePath, "temp_image_${UUID.randomUUID()}.jpg")
            
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
            }
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    companion object {
        const val REQUEST_EDIT_IMAGE = 1001
    }
}
