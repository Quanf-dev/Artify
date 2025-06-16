package com.example.artify.ui.editbase

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigator
import androidx.viewbinding.ViewBinding
import com.example.artify.R
import com.example.artify.constants.Constants
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

// Extension functions for animations
fun View.slideUp() {
    this.visibility = View.VISIBLE
    val animation = AnimationUtils.loadAnimation(this.context, R.anim.slide_up)
    this.startAnimation(animation)
}


fun View.scaleIn() {
    this.visibility = View.VISIBLE
    val animation = AnimationUtils.loadAnimation(this.context, R.anim.scale_in)
    this.startAnimation(animation)
}


fun View.animateImageIn() {
    // Reset trạng thái ban đầu
    alpha = 0f
    scaleX = 0.5f
    scaleY = 0.5f

    // Dùng PathInterpolator để có cảm giác mềm mại hơn Overshoot
    val interpolator = PathInterpolator(0.2f, 0f, 0f, 1f) // tương đương cubic-bezier

    animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setStartDelay(50) // tạo chiều sâu nhẹ
        .setDuration(600) // mượt và đủ cảm giác
        .setInterpolator(interpolator)
        .start()
}


abstract class BaseEditActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var baseBinding: ActivityBaseEditBinding

    protected lateinit var binding: VB
    protected var currentImageBitmap: Bitmap? = null

    abstract fun inflateBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Đảm bảo cửa sổ có nền đục (không trong suốt)
        window.setBackgroundDrawableResource(android.R.color.white)
        
        baseBinding = ActivityBaseEditBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = inflateBinding()
        baseBinding.contentContainerEdit.addView(binding.root)




    }

    // Phương thức để hiển thị bottom bar với animation
    protected fun animateBottomBar(view: View) {
        // Đảm bảo view hiện tại ẩn
        view.visibility = View.INVISIBLE
        
        // Delay một chút để activity hiển thị trước
        view.postDelayed({
            view.slideUp()
        }, 100)
    }

    // Phương thức để tạo hiệu ứng ripple cho các nút
    protected fun setupRippleEffects(vararg views: View) {
        for (view in views) {
            view.isClickable = true
            view.isFocusable = true
            
            // Đặt background với hiệu ứng ripple nếu chưa có
            if (view.background == null) {
                val outValue = android.util.TypedValue()
                this.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                view.setBackgroundResource(outValue.resourceId)
            }
        }
    }

    protected open fun updateCurrentImageBitmapFromContainer() {}

    protected fun navigateToEditActivity(updateBitmap: Boolean = true, targetActivity: Class<out Activity>) {
        if (updateBitmap) updateCurrentImageBitmapFromContainer()
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        val intent = Intent(this, targetActivity)
        intent.putExtra("image_path", tempFile?.absolutePath)
        startActivityForResult(intent, REQUEST_EDIT_IMAGE)
    }

    protected fun navigateToPaint() = navigateToEditActivity(true, PaintActivity::class.java)
    protected fun navigateToFrame() = navigateToEditActivity(true, FrameActivity::class.java)
    protected fun navigateToCrop() = navigateToEditActivity(true, CropActivity::class.java)
    protected fun navigateToTune() = navigateToEditActivity(true, ImageTuneActivity::class.java)
    protected fun navigateToFilter() = navigateToEditActivity(true, FilterActivity::class.java)
    protected fun navigateToBlur() = navigateToEditActivity(true, BlurActivity::class.java)
    protected fun navigateToEmoji() = navigateToEditActivity(true, StickerActivity::class.java)

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

    protected fun saveBitmapToTempFile(bitmap: Bitmap?): File? {
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

    /**
     * Nhận bitmap đầu vào từ intent (ưu tiên image_path, sau đó share intent, fallback ảnh mẫu nếu cần)
     */
    protected fun getInputBitmap(onBitmapReady: (Bitmap) -> Unit, onError: (() -> Unit)? = null) {
        // Kiểm tra image_path từ PreviewActivity hoặc các activity khác
        val imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    onBitmapReady(bitmap)
                    return
                }
            } catch (e: Exception) {
                Log.e("BaseEditActivity", "Error loading from image_path: ${e.message}", e)
            }
        }
        
        // Kiểm tra image_path từ Constants
        val constantsImagePath = intent.getStringExtra(Constants.EXTRA_IMAGE_PATH)
        if (!constantsImagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(constantsImagePath)
            if (bitmap != null) {
                onBitmapReady(bitmap)
                return
            }
        }
        
        // Nếu không có image_path, thử lấy từ share intent
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (imageUri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        onBitmapReady(bitmap)
                        return
                    }
                } catch (e: Exception) {
                    Log.e("BaseEditActivity", "Error loading from intent URI: ${e.message}", e)
                }
            }
        }
        
        // Nếu vẫn không có, gọi onError hoặc load ảnh mẫu
        onError?.invoke()
    }

    companion object {
        const val REQUEST_EDIT_IMAGE = 1001
    }
}
