package com.example.artify.ui.paint

import android.app.Dialog
import com.example.artify.R
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.artify.databinding.ActivityPaintzBinding
import com.example.artify.databinding.DialogPaintSettingsBinding
import com.example.artify.databinding.ItemToolbarPaintBinding
import com.example.artify.databinding.ItemBottomPaintBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.tools.base.BlurTool
import com.example.imageeditor.tools.base.EraserTool
import com.example.imageeditor.tools.base.SelectionTool
import com.example.imageeditor.tools.freestyle.FreestyleTool
import com.example.imageeditor.tools.shapes.ArrowTool
import com.example.imageeditor.tools.shapes.CircleTool
import com.example.imageeditor.tools.shapes.DashLineTool
import com.example.imageeditor.tools.shapes.LineTool
import com.example.imageeditor.tools.shapes.RectangleTool
import com.example.imageeditor.tools.text.TextTool
import com.example.imageeditor.tools.text.TextSelectionTool
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PaintActivity : BaseEditActivity<ActivityPaintzBinding>() {
    private lateinit var toolbarBinding: ItemToolbarPaintBinding
    private lateinit var bottomBinding: ItemBottomPaintBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var currentSelectedColor: Int = Color.BLACK
    private var currentBlurTool: BlurTool? = null
    private var paintSettingsDialog: Dialog? = null
    private var dialogBinding: DialogPaintSettingsBinding? = null
    private var currentEraserTool: EraserTool? = null
    private var currentTextTool: TextTool? = null
    private var currentTextSelectionTool: TextSelectionTool? = null
    private var textEditDialog: Dialog? = null
    private var currentTextColor = Color.BLACK
    private var currentTextBgColor = Color.WHITE
    private var currentTextBgAlpha = 128 // 50% opacity

    override fun inflateBinding(): ActivityPaintzBinding {
        return ActivityPaintzBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize bindings
        toolbarBinding = ItemToolbarPaintBinding.bind(binding.root.findViewById(R.id.toolbarPaint))
        bottomBinding = ItemBottomPaintBinding.bind(binding.root.findViewById(R.id.bottomPaint))

        setupImagePicker()
        setupPaintEditorView()
        setupToolSelectionButtons()
        setupBlurView()
        setupPaintSettingsDialog()
        setupTextEditDialog()
        setupClickListeners()

        // Set default tool to Freestyle
        binding.paintEditorView.setTool(FreestyleTool())
        updateToolSelection(bottomBinding.llFreeStyle)

        // Ưu tiên nhận image_path từ Intent
        val imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                binding.paintEditorView.setBackgroundImage(bitmap)
            }
        }
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri: Uri ->
                    try {
                        val inputStream: InputStream? = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.paintEditorView.setBackgroundImage(bitmap)
                        inputStream?.close()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun setupPaintEditorView() {
        binding.paintEditorView.setColor(currentSelectedColor)
        
        // Thiết lập TextSelectionTool
        setupTextSelectionTool()
    }
    
    private fun setupTextSelectionTool() {
        currentTextSelectionTool = TextSelectionTool(
            binding.paintEditorView.getLayerManager(),
            view = binding.paintEditorView
        )
        
        // Xử lý sự kiện khi văn bản được chọn
        currentTextSelectionTool?.setOnTextSelectedListener { textDrawable ->
            // Cập nhật UI khi văn bản được chọn
        }
        
        // Xử lý sự kiện khi yêu cầu chỉnh sửa văn bản
        currentTextSelectionTool?.setOnTextEditRequestListener { textDrawable ->
            // Hiển thị dialog chỉnh sửa với thông tin văn bản hiện tại
            val dialogView = textEditDialog?.findViewById<View>(android.R.id.content)?.rootView
            dialogView?.let {
                val etText = it.findViewById<EditText>(R.id.etText)
                val spinnerFont = it.findViewById<Spinner>(R.id.spinnerFont)
                val seekBarTextSize = it.findViewById<SeekBar>(R.id.seekBarTextSize)
                val radioGroupAlign = it.findViewById<RadioGroup>(R.id.radioGroupAlign)
                val viewTextColor = it.findViewById<View>(R.id.viewTextColor)
                val viewBgColor = it.findViewById<View>(R.id.viewBgColor)
                val seekBarBgOpacity = it.findViewById<SeekBar>(R.id.seekBarBgOpacity)
                
                // Đặt giá trị hiện tại của văn bản
                etText.setText(textDrawable.text)
                
                // Đặt font chữ
                val fontIndex = when (textDrawable.typeface) {
                    Typeface.SANS_SERIF -> 1
                    Typeface.SERIF -> 2
                    Typeface.MONOSPACE -> 3
                    else -> 0
                }
                spinnerFont.setSelection(fontIndex)
                
                // Đặt kích thước chữ (chuyển đổi từ kích thước thực tế sang giá trị seekbar)
                val sizeProgress = ((textDrawable.textSize - 20f) * 2).toInt().coerceIn(0, 100)
                seekBarTextSize.progress = sizeProgress
                
                // Đặt căn lề
                val alignId = when (textDrawable.align) {
                    Paint.Align.LEFT -> R.id.rbLeft
                    Paint.Align.CENTER -> R.id.rbCenter
                    Paint.Align.RIGHT -> R.id.rbRight
                    else -> R.id.rbLeft
                }
                radioGroupAlign.check(alignId)
                
                // Đặt màu chữ và màu nền
                viewTextColor.setBackgroundColor(textDrawable.color)
                viewBgColor.setBackgroundColor(textDrawable.backgroundColor)
                seekBarBgOpacity.progress = textDrawable.backgroundAlpha
                
                // Hiển thị dialog
                textEditDialog?.show()
            }
        }
        
        // Xử lý sự kiện khi yêu cầu xóa văn bản
        currentTextSelectionTool?.setOnTextDeleteRequestListener { textDrawable ->
            // Xóa văn bản khỏi layer
            binding.paintEditorView.getLayerManager().getActiveLayer()?.removeDrawable(textDrawable)
            binding.paintEditorView.redrawAllLayers()
        }
    }

    private fun setupBlurView() {
        val radius = 1f
        val parentView = binding.root.findViewById<ViewGroup>(R.id.mainContent)
        
        binding.blurView.setupWith(parentView, RenderScriptBlur(this))
            .setFrameClearDrawable(window.decorView.background)
            .setBlurRadius(radius)

        binding.overlayContainer.setOnClickListener {
            paintSettingsDialog?.dismiss()
        }
    }

    private fun setupPaintSettingsDialog() {
        paintSettingsDialog = Dialog(this, android.R.style.Theme_Material_Light_NoActionBar)
        dialogBinding = DialogPaintSettingsBinding.inflate(layoutInflater)
        paintSettingsDialog?.setContentView(dialogBinding?.root!!)

        paintSettingsDialog?.window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        paintSettingsDialog?.setCanceledOnTouchOutside(true)
        paintSettingsDialog?.setCancelable(true)

        paintSettingsDialog?.setOnDismissListener {
            binding.overlayContainer.visibility = View.GONE
        }

        dialogBinding?.btnCloseDialog?.setOnClickListener {
            paintSettingsDialog?.dismiss()
        }
        dialogBinding?.btnCloseDialogOpacity?.setOnClickListener {
            paintSettingsDialog?.dismiss()
        }

        dialogBinding?.seekBarLineWidth?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val width = progress.toFloat().coerceAtLeast(1f)
                binding.paintEditorView.setStrokeWidth(width)
                // Update eraser size if eraser tool is selected
                currentEraserTool?.setStrokeWidth(width)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding?.seekBarOpacity?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.paintEditorView.setOpacity(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    // Phương thức setupTextEditDialog() đã được chuyển xuống dưới

    private fun updateToolSelection(selectedView: View) {
        // Reset all tool backgrounds
        bottomBinding.llFreeStyle.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llLine.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llEraser.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llRectangle.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llCircle.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llArrow.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llDashLine.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llBlur.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llColor.setBackgroundResource(android.R.color.transparent)
        bottomBinding.llText.setBackgroundResource(android.R.color.transparent)

        // Set selected tool background
        selectedView.setBackgroundResource(R.drawable.bg_tool_selected)
    }

    private fun setupToolSelectionButtons() {
        // Basic tools
        bottomBinding.llFreeStyle.setOnClickListener {
            binding.paintEditorView.setTool(FreestyleTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Freestyle Tool Selected", Toast.LENGTH_SHORT).show()
        }

        bottomBinding.llLine.setOnClickListener {
            binding.paintEditorView.setTool(LineTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Line Tool Selected", Toast.LENGTH_SHORT).show()
        }

        bottomBinding.llEraser.setOnClickListener {
            currentEraserTool = EraserTool()
            currentEraserTool?.setStrokeWidth(30f)  // Set default eraser size
            binding.paintEditorView.setTool(currentEraserTool!!)
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Eraser Tool Selected", Toast.LENGTH_SHORT).show()
        }

        // Shape tools
        bottomBinding.llRectangle.setOnClickListener {
            binding.paintEditorView.setTool(RectangleTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Rectangle Tool Selected", Toast.LENGTH_SHORT).show()
        }

        bottomBinding.llCircle.setOnClickListener {
            binding.paintEditorView.setTool(CircleTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Circle Tool Selected", Toast.LENGTH_SHORT).show()
        }

        bottomBinding.llArrow.setOnClickListener {
            binding.paintEditorView.setTool(ArrowTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Arrow Tool Selected", Toast.LENGTH_SHORT).show()
        }

        bottomBinding.llDashLine.setOnClickListener {
            binding.paintEditorView.setTool(DashLineTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Dash Line Tool Selected", Toast.LENGTH_SHORT).show()
        }

        // Special tools
        bottomBinding.llBlur.setOnClickListener {
            currentBlurTool = BlurTool()
            binding.paintEditorView.setTool(currentBlurTool!!)
            updateToolUI(isBlurTool = true)
            updateToolSelection(it)
            Toast.makeText(this, "Blur Tool Selected", Toast.LENGTH_SHORT).show()
        }

        bottomBinding.llColor.setOnClickListener {
            updateToolSelection(it)
            ColorPickerDialog.Builder(this)
                .setTitle("Choose Color")
                .setPreferenceName("ImageEditorTestActivityColorPicker")
                .setPositiveButton(getString(R.string.ok),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                            envelope?.let {
                                currentSelectedColor = it.color
                                binding.paintEditorView.setColor(currentSelectedColor)
                            }
                        }
                    })
                .setNegativeButton(getString(R.string.cancel),
                    DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }

        bottomBinding.llClear.setOnClickListener { binding.paintEditorView.clear() }

        // Setup blur radius seekbar
        binding.seekBarBlurRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBlurTool?.setBlurRadius(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        bottomBinding.llSelect.setOnClickListener {
            // Chọn công cụ Selection cho các đối tượng thông thường
            val selectionTool = SelectionTool(
                binding.paintEditorView.getLayerManager(),
                view = binding.paintEditorView
            )
            binding.paintEditorView.setTool(selectionTool)
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Selection Tool Selected", Toast.LENGTH_SHORT).show()
        }
        
        // Thêm xử lý sự kiện nhấp đúp vào nút Select để chuyển sang TextSelectionTool
        bottomBinding.llSelect.setOnLongClickListener {
            // Chọn công cụ TextSelection cho các đối tượng văn bản
            binding.paintEditorView.setTool(currentTextSelectionTool!!)
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            Toast.makeText(this, "Text Selection Tool Selected", Toast.LENGTH_SHORT).show()
            true
        }
        
        // Text tool
        bottomBinding.llText.setOnClickListener {
            currentTextTool = TextTool()
            binding.paintEditorView.setTool(currentTextTool!!)
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
            
            // Hiển thị dialog chỉnh sửa văn bản
            textEditDialog?.show()
            
            // Tự động tạo văn bản ở giữa màn hình khi chọn công cụ Text
            // Tạo một MotionEvent giả để kích hoạt onTouchStart
            val downTime = SystemClock.uptimeMillis()
            val eventTime = SystemClock.uptimeMillis()
            val x = binding.paintEditorView.width / 2f
            val y = binding.paintEditorView.height / 2f
            val metaState = 0
            val motionEvent = MotionEvent.obtain(
                downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, metaState
            )
            currentTextTool?.onTouchStart(motionEvent)
            motionEvent.recycle()
            
            Toast.makeText(this, "Text Tool Selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToolUI(isBlurTool: Boolean) {
        binding.blurRadiusLayout.visibility = if (isBlurTool) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        toolbarBinding.btnFilterList.setOnClickListener {
            showLineWidthDialog()
        }

        toolbarBinding.btnBlurLinear.setOnClickListener {
            showOpacityDialog()
        }

        toolbarBinding.btnFormatColorReset.setOnClickListener {
            val isChecked = !it.isSelected
            it.isSelected = isChecked // Cập nhật lại trạng thái

            val newIconRes = if (isChecked) R.drawable.ic_raindrop else R.drawable.ic_format_color_reset
            toolbarBinding.btnFormatColorReset.setImageResource(newIconRes)

            binding.paintEditorView.setFillMode(isChecked)
        }

    }

    private fun showLineWidthDialog() {
        dialogBinding?.apply {
            containerLineWidth.visibility = View.VISIBLE
            containerOpacity.visibility = View.GONE
        }
        binding.overlayContainer.visibility = View.VISIBLE
        paintSettingsDialog?.show()
    }

    private fun showOpacityDialog() {
        dialogBinding?.apply {
            containerLineWidth.visibility = View.GONE
            containerOpacity.visibility = View.VISIBLE
        }
        binding.overlayContainer.visibility = View.VISIBLE
        paintSettingsDialog?.show()
    }

    private fun setupTextEditDialog() {
        textEditDialog = Dialog(this, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth)
        val dialogView = layoutInflater.inflate(R.layout.dialog_text_edit, null)
        textEditDialog?.setContentView(dialogView)
        
        // Lấy các view từ dialog
        val etText = dialogView.findViewById<EditText>(R.id.etText)
        val spinnerFont = dialogView.findViewById<Spinner>(R.id.spinnerFont)
        val seekBarTextSize = dialogView.findViewById<SeekBar>(R.id.seekBarTextSize)
        val radioGroupAlign = dialogView.findViewById<RadioGroup>(R.id.radioGroupAlign)
        val rbLeft = dialogView.findViewById<RadioButton>(R.id.rbLeft)
        val rbCenter = dialogView.findViewById<RadioButton>(R.id.rbCenter)
        val rbRight = dialogView.findViewById<RadioButton>(R.id.rbRight)
        val viewTextColor = dialogView.findViewById<View>(R.id.viewTextColor)
        val viewBgColor = dialogView.findViewById<View>(R.id.viewBgColor)
        val seekBarBgOpacity = dialogView.findViewById<SeekBar>(R.id.seekBarBgOpacity)

        // Thiết lập adapter cho spinner font
        val fontNames = arrayOf("Default", "Sans Serif", "Serif", "Monospace")
        val fontAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontNames)
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFont.adapter = fontAdapter
        
        // Xử lý sự kiện khi nhấn nút hủy
//        btnCancel.setOnClickListener {
//            textEditDialog?.dismiss()
//        }
        
        // Xử lý sự kiện khi nhấn nút áp dụng
//        btnApply.setOnClickListener {
//            val selectedText = etText.text.toString()
//            if (selectedText.isNotEmpty()) {
//                // Lấy các thuộc tính đã chọn
//                val textSize = 20f + seekBarTextSize.progress.toFloat() / 2
//
//                // Xác định căn lề
//                val textAlign = when (radioGroupAlign.checkedRadioButtonId) {
//                    R.id.rbLeft -> Paint.Align.LEFT
//                    R.id.rbCenter -> Paint.Align.CENTER
//                    R.id.rbRight -> Paint.Align.RIGHT
//                    else -> Paint.Align.LEFT
//                }
//
//                // Xác định font chữ
//                val typeface = when (spinnerFont.selectedItemPosition) {
//                    0 -> Typeface.DEFAULT
//                    1 -> Typeface.SANS_SERIF
//                    2 -> Typeface.SERIF
//                    3 -> Typeface.MONOSPACE
//                    else -> Typeface.DEFAULT
//                }
//
//                // Lấy màu chữ và màu nền
//                val textColor = (viewTextColor.background as? ColorDrawable)?.color ?: Color.BLACK
//                val bgColor = (viewBgColor.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
//                val bgAlpha = seekBarBgOpacity.progress
//
//                // Cập nhật TextTool
//                currentTextTool?.apply {
//                    setText(selectedText)
//                    setColor(textColor)
//                    setTextSize(textSize)
//                    setTextAlign(textAlign)
//                    setTypeface(typeface)
//                    setBackgroundColor(bgColor)
//                    setBackgroundAlpha(bgAlpha)
//                    setOpacity(255) // Mặc định độ mờ văn bản là 100%
//
//                    // Tự động thêm văn bản vào layer sau khi cập nhật thuộc tính
//                    val drawableItem = createDrawableItem()
//                    if (drawableItem != null) {
//                        binding.paintEditorView.getLayerManager().getActiveLayer()?.addDrawable(drawableItem)
//                        binding.paintEditorView.redrawAllLayers()
//                        binding.paintEditorView.invalidate()
//                    }
//                }
//
//                textEditDialog?.dismiss()
//            } else {
//                Toast.makeText(this@PaintActivity, "Vui lòng nhập văn bản", Toast.LENGTH_SHORT).show()
//            }
//        }
        
        // Xử lý sự kiện khi nhấn vào màu chữ
        viewTextColor.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("Chọn màu chữ")
                .setPreferenceName("TextColorPicker")
                .setPositiveButton(getString(R.string.ok),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                            envelope?.let {
                                viewTextColor.setBackgroundColor(it.color)
                            }
                        }
                    })
                .setNegativeButton(getString(R.string.cancel),
                    DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }
        
        // Xử lý sự kiện khi nhấn vào màu nền
        viewBgColor.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("Chọn màu nền")
                .setPreferenceName("TextBgColorPicker")
                .setPositiveButton(getString(R.string.ok),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                            envelope?.let {
                                viewBgColor.setBackgroundColor(it.color)
                            }
                        }
                    })
                .setNegativeButton(getString(R.string.cancel),
                    DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        paintSettingsDialog?.dismiss()
        textEditDialog?.dismiss()
        paintSettingsDialog = null
        textEditDialog = null
        dialogBinding = null
    }
}