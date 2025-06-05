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
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commit
import com.example.artify.R
import com.example.artify.databinding.ActivityEditMainBinding
import com.example.artify.databinding.ItemBottomBarEdtMainBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import android.graphics.drawable.GradientDrawable
import com.example.artify.model.TextProperties

class EditMainActivity : BaseEditActivity<ActivityEditMainBinding>() {
    private var imageUri: Uri? = null
    private lateinit var bottomBarBinding: ItemBottomBarEdtMainBinding
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val inputStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                binding.editorView.setImageBitmap(bitmap)
            }
        }
    }

    override fun inflateBinding(): ActivityEditMainBinding {
        return ActivityEditMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomBarBinding = ItemBottomBarEdtMainBinding.bind(binding.root)
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root)

        setupClickListeners()
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

        toolbarBinding.root.findViewById<View>(R.id.tbEdtMain)?.setOnClickListener {
            onBackPressed()
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