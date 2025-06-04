package com.example.artify.ui.editMain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commit
import com.example.artify.R
import miaoyongjun.stickerview.StickerView
import android.graphics.drawable.GradientDrawable
import com.example.artify.model.TextProperties

class EditMainActivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var stickerView: StickerView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val inputStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap) // Display picked image
                // Optionally save to a temporary file if needed for other purposes
                // val file = File(cacheDir, "editing_image.png")
                // FileOutputStream(file).use { fos ->
                //     bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                // }
                // imageUri = Uri.fromFile(file)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_main)

        imageView = findViewById(R.id.editorView) // Assuming your ImageView in activity_edit_main is editorView
        stickerView = findViewById(R.id.sticker_view)

        // Button to pick image (assuming you have one in your main layout)
//        findViewById<View>(R.id.btnPickImage)?.setOnClickListener { // Replace R.id.btnPickImage with your actual button ID
//            pickImage()
//        }

        findViewById<View>(R.id.llText)?.setOnClickListener {
            showEditTextFragment()
        }
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun showEditTextFragment() {
        val fragment = EditTextFragment()
        fragment.onTextPropertiesChanged = {
            addTextOverlay(it)
        }
        supportFragmentManager.commit {
            replace(R.id.textFragmentContainer, fragment)
            addToBackStack(null) // Optional: allows user to press back to close fragment
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
            // Tạo drawable bo góc cho background
            val radiusPx = 15f * resources.displayMetrics.density
            val bgDrawable = GradientDrawable().apply {
                cornerRadius = radiusPx
                setColor(bgColorWithAlpha)
            }
            background = bgDrawable

            // Set width/height giống preview
            layoutParams = android.widget.FrameLayout.LayoutParams(properties.viewWidth, properties.viewHeight)

            // Measure and layout the TextView to get its dimensions for the bitmap
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
        stickerView.addSticker(drawable)
    }

}