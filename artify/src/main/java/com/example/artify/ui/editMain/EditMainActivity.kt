package com.example.artify.ui.editMain

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import com.example.artify.ui.editbase.BaseEditActivity
import android.graphics.drawable.GradientDrawable
import android.Manifest
import android.content.pm.PackageManager
import com.example.artify.model.TextProperties
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditMainActivity : BaseEditActivity<ActivityEditMainBinding>() {
    private var imageUri: Uri? = null
    private lateinit var bottomBarBinding: ItemBottomBarEdtMainBinding
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            saveImage()
        } else {
            Toast.makeText(this, "Storage permission is required to save images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun inflateBinding(): ActivityEditMainBinding {
        return ActivityEditMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomBarBinding = ItemBottomBarEdtMainBinding.bind(binding.root)
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root)

        // Get image URI from intent
        val uriString = intent.getStringExtra("image_uri")
        if (uriString != null) {
            imageUri = Uri.parse(uriString)
            loadImageFromUri(imageUri)
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

    private fun loadImageFromUri(uri: Uri?) {
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    currentImageBitmap = bitmap
                    binding.editorView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        bottomBarBinding.llText.setOnClickListener {
            showEditTextFragment()
        }

        bottomBarBinding.llPaint.setOnClickListener {
            navigateToPaint()
        }

        bottomBarBinding.llCrop.setOnClickListener {
            navigateToCrop()
        }

        bottomBarBinding.llTune.setOnClickListener {
            navigateToTune()
        }

        bottomBarBinding.llFilter.setOnClickListener {
            navigateToFilter()
        }

        bottomBarBinding.llBlur.setOnClickListener {
            navigateToBlur()
        }

        bottomBarBinding.llEmoji.setOnClickListener {
            navigateToEmoji()
        }

        toolbarBinding.ivDone.setOnClickListener {
            checkPermissionsAndSave()
        }

        toolbarBinding.root.setOnClickListener {
            finish() // Return to HomeActivity
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
        // Create bitmap from the image view and stickers
        val bitmap = createFinalBitmap() ?: return

        try {
            var fos: OutputStream? = null
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "Artify_" + timeStamp

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$imageFileName.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Artify")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { fos = contentResolver.openOutputStream(it) }
            } else {
                // For Android 9 and below
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Artify")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val image = File(imagesDir, "$imageFileName.jpg")
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
                // Return to home activity
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFinalBitmap(): Bitmap? {
        try {
            // Get the base image from ImageView
            val baseBitmap = currentImageBitmap ?: return null
            
            // Create a new bitmap with the same dimensions
            val resultBitmap = Bitmap.createBitmap(
                baseBitmap.width,
                baseBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            
            // Draw the base image
            val canvas = android.graphics.Canvas(resultBitmap)
            canvas.drawBitmap(baseBitmap, 0f, 0f, null)
            
            // Draw any stickers
            binding.stickerView.draw(canvas)
            
            return resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun showEditTextFragment() {
        val fragment = EditTextFragment()
        fragment.onTextPropertiesChanged = {
            addTextOverlay(it)
        }
        supportFragmentManager.commit {
            replace(R.id.textFragmentContainer, fragment)
            addToBackStack(null)
        }
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

            val bgColorWithAlpha = Color.argb(
                properties.backgroundAlpha,
                Color.red(properties.backgroundColor),
                Color.green(properties.backgroundColor),
                Color.blue(properties.backgroundColor)
            )
            val radiusPx = 15f * resources.displayMetrics.density
            val bgDrawable = GradientDrawable().apply {
                cornerRadius = radiusPx
                setColor(bgColorWithAlpha)
            }
            background = bgDrawable

            layoutParams = android.widget.FrameLayout.LayoutParams(properties.viewWidth, properties.viewHeight)

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
        val canvas = android.graphics.Canvas(bitmap)
        textView.draw(canvas)
        val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
        binding.stickerView.addSticker(drawable)
    }
}