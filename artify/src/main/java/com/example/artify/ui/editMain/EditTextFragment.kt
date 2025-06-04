package com.example.artify.ui.editMain

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat
import com.example.artify.R
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import android.graphics.drawable.GradientDrawable
import androidx.core.widget.doOnTextChanged

data class TextProperties(
    var text: String = "",
    var fontResId: Int = 0, // 0 for default
    var textSizePx: Float = 60f, // Default pixel size
    var alignment: Paint.Align = Paint.Align.CENTER,
    var textColor: Int = Color.WHITE,
    var backgroundColor: Int = Color.TRANSPARENT,
    var backgroundAlpha: Int = 100, // Default 100 (0-255)
    var viewWidth: Int = 0,
    var viewHeight: Int = 0
)

class EditTextFragment : Fragment(R.layout.fragment_edit_text) {

    private lateinit var editText: EditText
    private lateinit var spinnerFontFamily: Spinner
    private lateinit var seekBarTextSize: SeekBar
    private lateinit var radioGroupAlign: RadioGroup
    private lateinit var btnTextColor: Button
    private lateinit var viewTextColorPreview: View
    private lateinit var btnBgColor: Button
    private lateinit var viewBgColorPreview: View
    private lateinit var seekBarBgOpacity: SeekBar
    private lateinit var btnDone: Button

    private val currentTextProperties = TextProperties()
    
    private val MIN_TEXT_SIZE = 20f
    private val MAX_TEXT_SIZE = 120f

    // Callback to pass properties to Activity
    var onTextPropertiesChanged: ((TextProperties) -> Unit)? = null

    // Map user-friendly names to actual font resource IDs
    // Ensure these resource IDs match the font files in your res/font folder
    private val fontMap = mapOf(
        "Default" to 0,
//        "Roboto" to R.font.roboto_regular, // Example: replace with your actual font file
//        "Open Sans" to R.font.opensans_regular, // Example
//        "Lato" to R.font.lato_regular, // Example
//        "Montserrat" to R.font.montserrat_regular, // Example
//        "Slabo 27px" to R.font.slabo_27px, // Example
//        "Raleway" to R.font.raleway_regular, // Example
//        "Merriweather" to R.font.merriweather_regular, // Example
//        "PT Sans" to R.font.ptsans_regular, // Example
//        "Lobster" to R.font.lobster_regular, // Example
//        "Pacifico" to R.font.pacifico_regular, // Example
//        "Caveat" to R.font.caveat_regular, // Example
//        "Dancing Script" to R.font.dancingscript_regular // Example
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editText = view.findViewById(R.id.editText)
        spinnerFontFamily = view.findViewById(R.id.spinnerFontFamily)
        seekBarTextSize = view.findViewById(R.id.seekBarTextSize)
        radioGroupAlign = view.findViewById(R.id.radioGroupAlign)
        btnTextColor = view.findViewById(R.id.btnTextColor)
        viewTextColorPreview = view.findViewById(R.id.viewTextColorPreview)
        btnBgColor = view.findViewById(R.id.btnBgColor)
        viewBgColorPreview = view.findViewById(R.id.viewBgColorPreview)
        seekBarBgOpacity = view.findViewById(R.id.seekBarBgOpacity)
        btnDone = view.findViewById(R.id.btnDone)

        editText.doOnTextChanged { text, _, _, _ ->
            // fix bug preview background ko bo vao text
            if (text.isNullOrEmpty()) {
                editText.hint = "Enter Text..."
            } else {
                editText.hint = null
            }
        }
        setupFontSpinner()
        setupControls()
        loadInitialProperties()
        setupTextWatcher()

        btnDone.setOnClickListener {
            currentTextProperties.text = editText.text.toString()
            currentTextProperties.viewWidth = editText.width
            currentTextProperties.viewHeight = editText.height
            onTextPropertiesChanged?.invoke(currentTextProperties)
            parentFragmentManager.beginTransaction().remove(this@EditTextFragment).commit()
        }
    }

    private fun setupTextWatcher() {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                autoAdjustTextSize()
            }
        })
    }

    private fun autoAdjustTextSize() {
        val text = editText.text.toString()
        if (text.isEmpty()) return

        val maxWidth = editText.width - editText.paddingLeft - editText.paddingRight
        val maxHeight = editText.height - editText.paddingTop - editText.paddingBottom

        if (maxWidth <= 0 || maxHeight <= 0) return

        var size = MAX_TEXT_SIZE
        val paint = editText.paint
        val bounds = android.graphics.Rect()
        
        while (size > MIN_TEXT_SIZE) {
            paint.textSize = size
            paint.getTextBounds(text, 0, text.length, bounds)
            
            if (bounds.width() <= maxWidth && bounds.height() <= maxHeight) {
                break
            }
            size -= 1f
        }

        // Update the seekbar to reflect the auto-adjusted size
        val progress = ((size - MIN_TEXT_SIZE) / (MAX_TEXT_SIZE - MIN_TEXT_SIZE) * 100).toInt()
        seekBarTextSize.progress = progress
        currentTextProperties.textSizePx = size
        updateEditTextPreview()
    }

    private fun setupFontSpinner() {
        val fontDisplayNames = resources.getStringArray(R.array.font_names)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, fontDisplayNames)
        spinnerFontFamily.adapter = adapter

        spinnerFontFamily.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedFontName = fontDisplayNames[position]
                currentTextProperties.fontResId = fontMap[selectedFontName] ?: 0
                updateEditTextPreview()
                autoAdjustTextSize()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupControls() {
        // Text Size
        seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val size = MIN_TEXT_SIZE + (progress / 100f) * (MAX_TEXT_SIZE - MIN_TEXT_SIZE)
                    currentTextProperties.textSizePx = size
                    updateEditTextPreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Alignment
        radioGroupAlign.setOnCheckedChangeListener { _, checkedId ->
            currentTextProperties.alignment = when (checkedId) {
                R.id.rbLeft -> Paint.Align.LEFT
                R.id.rbRight -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            updateEditTextPreview()
        }

        // Text Color
        btnTextColor.setOnClickListener {
            showColorPicker("Text Color", currentTextProperties.textColor) { color ->
                currentTextProperties.textColor = color
                viewTextColorPreview.setBackgroundColor(color)
                updateEditTextPreview()
            }
        }

        // Background Color
        btnBgColor.setOnClickListener {
            showColorPicker("Background Color", currentTextProperties.backgroundColor) { color ->
                currentTextProperties.backgroundColor = color
                updateBackgroundPreview()
            }
        }

        // Background Opacity
        seekBarBgOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentTextProperties.backgroundAlpha = progress
                updateBackgroundPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateBackgroundPreview() {
        val bgColorWithAlpha = Color.argb(
            currentTextProperties.backgroundAlpha,
            Color.red(currentTextProperties.backgroundColor),
            Color.green(currentTextProperties.backgroundColor),
            Color.blue(currentTextProperties.backgroundColor)
        )
        val radiusPx = 15f * resources.displayMetrics.density
        // Tạo 2 drawable riêng biệt cho preview và editText
        val bgDrawablePreview = GradientDrawable().apply {
            cornerRadius = radiusPx
            setColor(bgColorWithAlpha)
        }
        val bgDrawableEditText = GradientDrawable().apply {
            cornerRadius = radiusPx
            setColor(bgColorWithAlpha)
        }
        viewBgColorPreview.background = bgDrawablePreview
        editText.background = bgDrawableEditText
        // Thêm padding nhỏ để background bo góc ôm sát chữ
        val paddingPx = (8 * resources.displayMetrics.density).toInt()
        editText.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
    }

    private fun loadInitialProperties() {
        editText.setText(currentTextProperties.text)
        
        val fontDisplayNames = resources.getStringArray(R.array.font_names)
        val initialFontName = fontMap.entries.firstOrNull { it.value == currentTextProperties.fontResId }?.key ?: "Default"
        val initialFontPosition = fontDisplayNames.indexOf(initialFontName).coerceAtLeast(0)
        spinnerFontFamily.setSelection(initialFontPosition)
        
        // Convert textSizePx to progress
        val progress = ((currentTextProperties.textSizePx - MIN_TEXT_SIZE) / (MAX_TEXT_SIZE - MIN_TEXT_SIZE) * 100).toInt()
        seekBarTextSize.progress = progress.coerceIn(0, 100)

        val initialAlignId = when (currentTextProperties.alignment) {
            Paint.Align.LEFT -> R.id.rbLeft
            Paint.Align.RIGHT -> R.id.rbRight
            else -> R.id.rbCenter
        }
        radioGroupAlign.check(initialAlignId)

        viewTextColorPreview.setBackgroundColor(currentTextProperties.textColor)
        updateBackgroundPreview()
        seekBarBgOpacity.progress = currentTextProperties.backgroundAlpha
    }

    private fun showColorPicker(title: String, initialColor: Int, onColorSelected: (Int) -> Unit) {
        ColorPickerDialog.Builder(requireContext())
            .setTitle(title)
            .setPreferenceName(title.replace(" ", "") + "Picker")
            .setPositiveButton(getString(android.R.string.ok),
                ColorEnvelopeListener { envelope, _ -> onColorSelected(envelope.color) })
            .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .show()
    }

    private fun updateEditTextPreview() {
        // Font
        if (currentTextProperties.fontResId != 0) {
            try {
                editText.typeface = ResourcesCompat.getFont(requireContext(), currentTextProperties.fontResId)
            } catch (e: Exception) {
                editText.typeface = Typeface.DEFAULT
            }
        } else {
            editText.typeface = Typeface.DEFAULT
        }

        // Size - Convert to SP for preview
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTextProperties.textSizePx)
        
        // Align
        editText.gravity = when (currentTextProperties.alignment) {
            Paint.Align.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            Paint.Align.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
            else -> Gravity.CENTER
        }
        
        // Text color
        editText.setTextColor(currentTextProperties.textColor)

        // Cập nhật lại background bo góc
        updateBackgroundPreview()
    }
} 