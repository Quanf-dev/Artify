package com.example.artify.ui.editMain

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat
import com.example.artify.R
import com.example.artify.databinding.FragmentEditTextBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import android.graphics.drawable.GradientDrawable
import androidx.core.widget.doOnTextChanged
import com.example.artify.model.TextProperties
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.sizeDp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater

class EditTextFragment : Fragment() {
    private var _binding: FragmentEditTextBinding? = null
    private val binding get() = _binding!!

    private val currentTextProperties = TextProperties()
    
    private val MIN_TEXT_SIZE = 20f
    private val MAX_TEXT_SIZE = 120f

    // Callback to pass properties to Activity
    var onTextPropertiesChanged: ((TextProperties) -> Unit)? = null

    // Callback to show/hide toolbar in activity
    var onShowHideToolbar: ((Boolean) -> Unit)? = null

    // Font list and map
    private val fontList = listOf(
        "Default",
        "Bebas Neue",
        "Bungee Spice",
        "Unifraktur Cook",
        "Work Sans",
        "Playwrite IT Moderna Guides",
        "Kapakana",
        "Noto Serif Dives Akuru",
        "WDXL Lubrifont TC"
    )
    private val fontMap = mapOf(
        "Default" to 0,
        "Bebas Neue" to R.font.bebas_neue_regular,
        "Bungee Spice" to R.font.bungee_spice_regular,
        "Unifraktur Cook" to R.font.unifraktur_cook_bold,
        "Work Sans" to R.font.work_sans,
        "Playwrite IT Moderna Guides" to R.font.playwrite_it_moderna_guides_regular,
        "Kapakana" to R.font.kapakana_variablefont_wght,
        "Noto Serif Dives Akuru" to R.font.noto_serif_dives_akuru_regular,
        "WDXL Lubrifont TC" to R.font.wdxl_lubrifont_tc_regular
    )
    private var selectedFontIndex = 0
    private lateinit var fontAdapter: FontAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onShowHideToolbar?.invoke(false)
        setupFontRecycler()
        binding.editText.doOnTextChanged { text, _, _, _ ->
            // fix bug preview background ko bo vao text
            if (text.isNullOrEmpty()) {
                binding.editText.hint = "Enter Text..."
            } else {
                binding.editText.hint = null
            }
        }
        setupControls()
        loadInitialProperties()
        setupTextWatcher()

        binding.btnDone.setOnClickListener {
            currentTextProperties.text = binding.editText.text.toString()
            currentTextProperties.viewWidth = binding.editText.width
            currentTextProperties.viewHeight = binding.editText.height
            onTextPropertiesChanged?.invoke(currentTextProperties)
            parentFragmentManager.beginTransaction().remove(this@EditTextFragment).commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onShowHideToolbar?.invoke(true)
        _binding = null
    }

    private fun setupTextWatcher() {
        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                autoAdjustTextSize()
            }
        })
    }

    private fun autoAdjustTextSize() {
        val text = binding.editText.text.toString()
        if (text.isEmpty()) return

        val maxWidth = binding.editText.width - binding.editText.paddingLeft - binding.editText.paddingRight
        val maxHeight = binding.editText.height - binding.editText.paddingTop - binding.editText.paddingBottom

        if (maxWidth <= 0 || maxHeight <= 0) return

        var size = MAX_TEXT_SIZE
        val paint = binding.editText.paint
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
        binding.seekBarTextSize.progress = progress
        currentTextProperties.textSizePx = size
        updateEditTextPreview()
    }

    private fun setupFontRecycler() {
        fontAdapter = FontAdapter(fontList, selectedFontIndex, fontMap) { index ->
            val oldIndex = selectedFontIndex
            selectedFontIndex = index
            fontAdapter.selectedIndex = index
            fontAdapter.notifyItemChanged(oldIndex)
            fontAdapter.notifyItemChanged(index)
            val fontName = fontList[index]
            currentTextProperties.fontResId = fontMap[fontName] ?: 0
            updateEditTextPreview()
            Handler(Looper.getMainLooper()).post {
                binding.fontRecycler.smoothScrollToPosition(index)
            }
        }
        binding.fontRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.fontRecycler.adapter = fontAdapter
        binding.fontRecycler.scrollToPosition(selectedFontIndex)
        // Auto-select center item when scroll stops
        binding.fontRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val center = recyclerView.width / 2
                    var minDistance = Int.MAX_VALUE
                    var centerPos = selectedFontIndex
                    for (i in layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()) {
                        val v = layoutManager.findViewByPosition(i) ?: continue
                        val viewCenter = (v.left + v.right) / 2
                        val distance = kotlin.math.abs(viewCenter - center)
                        if (distance < minDistance) {
                            minDistance = distance
                            centerPos = i
                        }
                    }
                    if (centerPos != selectedFontIndex) {
                        fontAdapter.onFontSelected(centerPos)
                    }
                }
            }
        })
    }

    private fun setupControls() {
        // Text Size
        binding.seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        binding.btnAlign.setOnClickListener {
            // Đổi alignment theo vòng tròn: CENTER -> LEFT -> RIGHT -> CENTER...
            currentTextProperties.alignment = when (currentTextProperties.alignment) {
                Paint.Align.CENTER -> Paint.Align.LEFT
                Paint.Align.LEFT -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }

            // Cập nhật icon tương ứng
            val newIcon = when (currentTextProperties.alignment) {
                Paint.Align.LEFT -> "gmi-format-align-left"
                Paint.Align.RIGHT -> "gmi-format-align-right"
                else -> "gmi-format-align-center"
            }
            binding.btnAlign.icon = IconicsDrawable(requireContext(), newIcon).apply {
                sizeDp = 22
            }
            updateEditTextPreview()
        }


        // Text Color
        binding.btnTextColor.setOnClickListener {
            showColorPicker("Text Color", currentTextProperties.textColor) { color ->
                currentTextProperties.textColor = color
//                binding.viewTextColorPreview.setBackgroundColor(color)
                updateEditTextPreview()
            }
        }

        // Background Color
        binding.btnBgColor.setOnClickListener {
            showColorPicker("Background Color", currentTextProperties.backgroundColor) { color ->
                currentTextProperties.backgroundColor = color
                updateBackgroundPreview()
            }
        }

        // Background Opacity
        binding.seekBarBgOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        val bgDrawableEditText = GradientDrawable().apply {
            cornerRadius = radiusPx
            setColor(bgColorWithAlpha)
        }
        binding.editText.background = bgDrawableEditText
        // Thêm padding nhỏ để background bo góc ôm sát chữ
        val paddingPx = (8 * resources.displayMetrics.density).toInt()
        binding.editText.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
    }

    private fun loadInitialProperties() {
        binding.editText.setText(currentTextProperties.text)
        
        // Convert textSizePx to progress
        val progress = ((currentTextProperties.textSizePx - MIN_TEXT_SIZE) / (MAX_TEXT_SIZE - MIN_TEXT_SIZE) * 100).toInt()
        binding.seekBarTextSize.progress = progress.coerceIn(0, 100)

        updateBackgroundPreview()
        binding.seekBarBgOpacity.progress = currentTextProperties.backgroundAlpha
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
                binding.editText.typeface = ResourcesCompat.getFont(requireContext(), currentTextProperties.fontResId)
            } catch (e: Exception) {
                binding.editText.typeface = Typeface.DEFAULT
            }
        } else {
            binding.editText.typeface = Typeface.DEFAULT
        }

        // Size - Convert to SP for preview
        binding.editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTextProperties.textSizePx)
        
        // Align
        binding.editText.gravity = when (currentTextProperties.alignment) {
            Paint.Align.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            Paint.Align.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
            else -> Gravity.CENTER
        }
        
        // Text color
        binding.editText.setTextColor(currentTextProperties.textColor)

        // Cập nhật lại background bo góc
        updateBackgroundPreview()
    }

}

