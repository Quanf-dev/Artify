package com.example.artify.ui.editMain

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commit
import com.example.artify.R
import com.example.artify.databinding.ActivityEditMainBinding
import com.example.artify.databinding.ItemBottomBarEdtMainBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.model.TextProperties
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.artify.ui.editbase.animateImageIn
import com.example.artify.ui.editbase.scaleIn
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.AlertDialog
import androidx.core.content.FileProvider

class EditMainActivity : BaseEditActivity<ActivityEditMainBinding>() {
    private var imageUri: Uri? = null
    private lateinit var bottomBarBinding: ItemBottomBarEdtMainBinding
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding
    private var originalBitmap: Bitmap? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            saveImage()
        } else {
            Toast.makeText(
                this,
                "Storage permission is required to save images",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun inflateBinding(): ActivityEditMainBinding {
        return ActivityEditMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bottomBarView = findViewById<android.widget.HorizontalScrollView>(R.id.bottomBar)
        bottomBarBinding = ItemBottomBarEdtMainBinding.bind(bottomBarView)
        val toolbarView = findViewById<android.widget.LinearLayout>(R.id.toolbar)
        toolbarBinding = ItemToolbarEditMainBinding.bind(toolbarView)
        
        // Ẩn bottom menu để chuẩn bị cho animation
        bottomBarView.visibility = View.INVISIBLE

        // Nhận ảnh đầu vào đồng bộ
        getInputBitmap(
            onBitmapReady = { bitmap ->
                currentImageBitmap = bitmap
                originalBitmap = bitmap.copy(bitmap.config!!, true)
                binding.editorView.setImageBitmap(bitmap)
                binding.editorView.animateImageIn()
                animateBottomBar(bottomBarView)
                
                // Log thông tin về ảnh đã nhận
                Log.d("EditMainActivity", "Bitmap loaded successfully: ${bitmap.width}x${bitmap.height}")
            },
            onError = {
                // Kiểm tra trường hợp nhận từ PreviewActivity (sử dụng image_path)
                val imagePath = intent.getStringExtra("image_path")
                if (imagePath != null) {
                    try {
                        Log.d("EditMainActivity", "Loading from image_path: $imagePath")
                        val bitmap = BitmapFactory.decodeFile(imagePath)
                        if (bitmap != null) {
                            currentImageBitmap = bitmap
                            originalBitmap = bitmap.copy(bitmap.config!!, true)
                            binding.editorView.setImageBitmap(bitmap)
                            binding.editorView.animateImageIn()
                            animateBottomBar(bottomBarView)
                            return@getInputBitmap
                        } else {
                            Log.e("EditMainActivity", "Failed to decode bitmap from file: $imagePath")
                        }
                    } catch (e: Exception) {
                        Log.e("EditMainActivity", "Error loading image from path: ${e.message}", e)
                    }
                }
                
                // Nếu không có image_uri (từ HomeActivity), load vào
                val uriString = intent.getStringExtra("image_uri")
                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    try {
                        Log.d("EditMainActivity", "Loading from image_uri: $uriString")
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (bitmap != null) {
                            currentImageBitmap = bitmap
                            originalBitmap = bitmap.copy(bitmap.config!!, true)
                            binding.editorView.setImageBitmap(bitmap)
                            binding.editorView.animateImageIn()
                            animateBottomBar(bottomBarView)
                            return@getInputBitmap
                        }
                    } catch (e: Exception) {
                        Log.e("EditMainActivity", "Error loading image from URI: ${e.message}", e)
                    }
                }
                // Nếu vẫn không có, có thể load ảnh mẫu hoặc báo lỗi
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                
                // Vẫn hiển thị bottom menu với animation
                animateBottomBar(bottomBarView)
            }
        )

        toolbarBinding.btnClose.setOnClickListener{
            finish()
        }

        with(toolbarBinding) {
            ivRedo.visibility = View.GONE
        }

        setupClickListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_IMAGE && resultCode == Activity.RESULT_OK) {
            val imagePath = data?.getStringExtra("edited_image_path")
            if (imagePath != null) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    currentImageBitmap = bitmap
                    binding.editorView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun setupClickListeners() {
        bottomBarBinding.llText.setOnClickListener {
            it.scaleIn()
            showEditTextFragment()
        }

        bottomBarBinding.llPaint.setOnClickListener {
            it.scaleIn()
            updateCurrentImageBitmapFromContainer()
            navigateToPaint()
        }

        bottomBarBinding.llFrame.setOnClickListener {
            it.scaleIn()
            updateCurrentImageBitmapFromContainer()
            navigateToFrame()
        }

        bottomBarBinding.llCrop.setOnClickListener {
            it.scaleIn()
            updateCurrentImageBitmapFromContainer()
            navigateToCrop()
        }

        bottomBarBinding.llTune.setOnClickListener {
            it.scaleIn()
            updateCurrentImageBitmapFromContainer()
            navigateToTune()
        }

        bottomBarBinding.llFilter.setOnClickListener {
            it.scaleIn()
            updateCurrentImageBitmapFromContainer()
            navigateToFilter()
        }

        bottomBarBinding.llBlur.setOnClickListener {
            it.scaleIn()
            updateCurrentImageBitmapFromContainer()
            navigateToBlur()
        }

        bottomBarBinding.llEmoji.setOnClickListener {
            it.scaleIn()
            updateCurrentImageBitmapFromContainer()
            navigateToEmoji()
        }

        toolbarBinding.ivDone.setOnClickListener {
            it.scaleIn()
            showCompletionOptionsDialog()
        }

        toolbarBinding.ivUndo.setOnClickListener {
            it.scaleIn()
            originalBitmap?.let {
                currentImageBitmap = it.copy(it.config!!, true)
                binding.stickerView.clearSticker()
                binding.editorView.setImageBitmap(currentImageBitmap)
            }
        }
    }

    private fun showCompletionOptionsDialog() {
        // Luôn cập nhật bitmap mới nhất từ container (bao gồm sticker/text)
        updateCurrentImageBitmapFromContainer()
        
        if (currentImageBitmap == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Save Image", "Edit with AI", "Share to Social")
        
        AlertDialog.Builder(this)
            .setTitle("Complete Editing")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkPermissionsAndSave() // Lưu ảnh
                    1 -> openEditWithAI() // Mở EditImageActivity
                    2 -> shareToSocial() // Mở CreatePostActivity
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEditWithAI() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        tempFile?.let {
            try {
                // Tạo URI từ file tạm để truyền cho EditImageActivity
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".provider",
                    it
                )
                
                val intent = Intent(this, com.example.imageaigen.ui.edit.EditImageActivity::class.java)
                
                // Truyền URI để EditImageActivity có thể trực tiếp load vào originalBitmap
                intent.putExtra("image_uri", fileUri.toString())
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                Log.d("EditMainActivity", "Opening EditImageActivity with URI: $fileUri")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("EditMainActivity", "Error opening EditImageActivity: ${e.message}", e)
                Toast.makeText(this, "Failed to open AI editor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Failed to prepare image for AI editing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareToSocial() {
        val tempFile = saveBitmapToTempFile(currentImageBitmap)
        tempFile?.let {
            try {
                // Tạo URI từ file tạm để truyền cho CreatePostActivity
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".provider",
                    it
                )
                
                // Tìm class CreatePostActivity trong package com.example.socialposts
                val createPostActivityClass = Class.forName("com.example.socialposts.ui.CreatePostActivity")
                val intent = Intent(this, createPostActivityClass)
                
                // Truyền URI để CreatePostActivity có thể trực tiếp load vào selectedImageUri và ivPostImage
                intent.putExtra("selected_image_uri", fileUri.toString())
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                Log.d("EditMainActivity", "Opening CreatePostActivity with URI: $fileUri")
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                Log.e("EditMainActivity", "CreatePostActivity not found: ${e.message}", e)
                Toast.makeText(this, "Social posting feature not available", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("EditMainActivity", "Error opening CreatePostActivity: ${e.message}", e)
                Toast.makeText(this, "Failed to share to social: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Failed to prepare image for sharing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ we don't need explicit storage permissions
            saveImage()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveImage()
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun saveImage() {
        // Luôn cập nhật bitmap mới nhất từ container (bao gồm sticker/text)
        updateCurrentImageBitmapFromContainer()

        val mergedBitmap = currentImageBitmap
        if (mergedBitmap == null) {
            Toast.makeText(this, "No image to save (bitmap null)", Toast.LENGTH_SHORT).show()
            android.util.Log.e("EditMainActivity", "currentImageBitmap null sau updateCurrentImageBitmapFromContainer")
            return
        }
        var fos: OutputStream? = null
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "Artify_" + timeStamp

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$imageFileName.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Artify")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri == null) {
                    Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("EditMainActivity", "contentResolver.insert trả về null")
                    return
                }
                fos = contentResolver.openOutputStream(uri)
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Artify")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val image = File(imagesDir, "$imageFileName.jpg")
                fos = FileOutputStream(image)
            }

            if (fos == null) {
                Toast.makeText(this, "Failed to open output stream", Toast.LENGTH_SHORT).show()
                android.util.Log.e("EditMainActivity", "fos là null")
                return
            }

            val success = mergedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            if (success) {
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
                android.util.Log.d("EditMainActivity", "Lưu ảnh thành công")
                finish()
            } else {
                Toast.makeText(this, "Failed to save image (compress error)", Toast.LENGTH_SHORT).show()
                android.util.Log.e("EditMainActivity", "compress trả về false")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("EditMainActivity", "Exception khi lưu ảnh: ${e.message}")
        } finally {
            try { fos?.close() } catch (_: Exception) {}
        }
    }

    fun getEditedBitmap(): Bitmap? {
        if (currentImageBitmap == null) return null

        // Tạo một bitmap mới để giữ kết quả
        val result = currentImageBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Dán sticker (đã loại control) lên canvas
        val stickerOnlyBitmap = binding.stickerView.saveSticker()
        canvas.drawBitmap(stickerOnlyBitmap, 0f, 0f, null)

        return result
    }

    private fun showEditTextFragment() {
        val fragment = EditTextFragment()
        fragment.onTextPropertiesChanged = {
            addTextOverlay(it)
        }
        fragment.onShowHideToolbar = { show ->
            val toolbarView = findViewById<android.widget.LinearLayout>(R.id.toolbar)
            toolbarView.visibility = if (show) View.VISIBLE else View.GONE
        }
        supportFragmentManager.commit {
            replace(R.id.textFragmentContainer, fragment)
            addToBackStack(null)
        }
        // Hide toolbar when fragment is shown
        val toolbarView = findViewById<android.widget.LinearLayout>(R.id.toolbar)
        toolbarView.visibility = View.GONE
    }

    private fun addTextOverlay(properties: TextProperties) {
        val textView = TextView(this).apply {
            text = properties.text
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, properties.textSizePx)
            setTextColor(properties.textColor)

            if (properties.fontResId != 0) {
                try {
                    typeface = ResourcesCompat.getFont(this@EditMainActivity, properties.fontResId)
                } catch (e: Exception) {
                    typeface = Typeface.DEFAULT
                }
            } else {
                typeface = Typeface.DEFAULT
            }

            gravity = when (properties.alignment) {
                Paint.Align.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
                Paint.Align.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
                else -> Gravity.CENTER
            }

            background = properties.backgroundMain


            layoutParams =
                android.widget.FrameLayout.LayoutParams(properties.viewWidth, properties.viewHeight)

            measure(
                View.MeasureSpec.makeMeasureSpec(properties.viewWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(properties.viewHeight, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, properties.viewWidth, properties.viewHeight)
        }

        if (textView.width == 0 || textView.height == 0) {
            return
        }

        val bitmap = Bitmap.createBitmap(textView.width, textView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        textView.draw(canvas)
        val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
        binding.stickerView.addSticker(drawable)
        binding.stickerView.minStickerSizeScale = 0.2f

    }

    // Thêm hàm tiện ích lấy bitmap từ view
    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        view.draw(canvas)
        return returnedBitmap
    }

    // Thêm hàm cập nhật currentImageBitmap từ containImage
    override fun updateCurrentImageBitmapFromContainer() {
        val container = findViewById<android.widget.ImageView>(R.id.editorView)
        if (container.width > 0 && container.height > 0) {
            currentImageBitmap = getBitmapFromView(container)
            currentImageBitmap = getEditedBitmap()
        }
    }
}