package com.example.artify.ui.editbase

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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

    protected open fun updateCurrentImageBitmapFromContainer() {}

    protected fun navigateToPaint() {
        updateCurrentImageBitmapFromContainer()
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, PaintActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToText() {
        updateCurrentImageBitmapFromContainer()
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, StickerActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToCrop() {
        updateCurrentImageBitmapFromContainer()
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, CropActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToTune() {
        updateCurrentImageBitmapFromContainer()
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, ImageTuneActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToFilter() {
        updateCurrentImageBitmapFromContainer()
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, FilterActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToBlur() {
        updateCurrentImageBitmapFromContainer()
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, BlurActivity::class.java)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToEmoji() {
        updateCurrentImageBitmapFromContainer()
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

    protected fun loadBitmapFromUri(uri: Uri?): Bitmap? {
        if (uri == null) return null
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    protected fun loadBitmapFromFilePath(path: String?): Bitmap? {
        if (path.isNullOrEmpty()) return null
        return try {
            val file = java.io.File(path)
            if (file.exists()) BitmapFactory.decodeFile(path) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    protected fun setImageToViewFromFilePath(path: String?, setBitmap: (Bitmap) -> Unit, onError: (() -> Unit)? = null) {
        val bitmap = loadBitmapFromFilePath(path)
        if (bitmap != null) {
            setBitmap(bitmap)
        } else {
            onError?.invoke()
        }
    }

    protected fun setImageToViewFromUri(uri: Uri?, setBitmap: (Bitmap) -> Unit, onError: (() -> Unit)? = null) {
        val bitmap = loadBitmapFromUri(uri)
        if (bitmap != null) {
            setBitmap(bitmap)
        } else {
            onError?.invoke()
        }
    }

    companion object {
        const val REQUEST_EDIT_IMAGE = 1001
    }
}
