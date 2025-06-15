package com.example.artify.ui.paint

import android.app.Dialog
import com.example.artify.R
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.artify.databinding.ActivityPaintBinding
import com.example.artify.databinding.DialogPaintSettingsBinding
import com.example.artify.databinding.ItemToolbarPaintBinding
import com.example.artify.databinding.ItemBottomPaintBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.artify.ui.editbase.animateImageIn
import com.example.artify.ui.editbase.scaleIn
import com.example.imageeditor.tools.base.BlurTool
import com.example.imageeditor.tools.base.EraserTool
import com.example.imageeditor.tools.base.SelectionTool
import com.example.imageeditor.tools.freestyle.FreestyleTool
import com.example.imageeditor.tools.shapes.ArrowTool
import com.example.imageeditor.tools.shapes.CircleTool
import com.example.imageeditor.tools.shapes.DashLineTool
import com.example.imageeditor.tools.shapes.LineTool
import com.example.imageeditor.tools.shapes.RectangleTool
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.InputStream

class   PaintActivity : BaseEditActivity<ActivityPaintBinding>() {
    private lateinit var toolbarBinding: ItemToolbarPaintBinding
    private lateinit var bottomBinding: ItemBottomPaintBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var currentSelectedColor: Int = Color.BLACK
    private var currentBlurTool: BlurTool? = null
    private var paintSettingsDialog: Dialog? = null
    private var dialogBinding: DialogPaintSettingsBinding? = null
    private var currentEraserTool: EraserTool? = null

    override fun inflateBinding(): ActivityPaintBinding {
        return ActivityPaintBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize bindings
        toolbarBinding = ItemToolbarPaintBinding.bind(binding.root.findViewById(R.id.toolbarPaint))
        bottomBinding = ItemBottomPaintBinding.bind(binding.root.findViewById(R.id.bottomPaint))

        // Ẩn bottom menu để chuẩn bị cho animation
        binding.root.findViewById<View>(R.id.bottomPaint).visibility = View.INVISIBLE

        setupImagePicker()
        setupPaintEditorView()
        setupToolSelectionButtons()
        setupBlurView()
        setupPaintSettingsDialog()
        setupClickListeners()

        // Set default tool to Freestyle
        binding.paintEditorView.setTool(FreestyleTool())
        updateToolSelection(bottomBinding.llFreeStyle)

        toolbarBinding.btnClose.setOnClickListener{
            finish()
        }

        // Ưu tiên nhận image_path từ Intent
        getInputBitmap(
            onBitmapReady = { bitmap ->
                binding.paintEditorView.setBackgroundImage(bitmap)
                binding.paintEditorView.animateImageIn()
                currentImageBitmap = bitmap
                animateBottomBar(binding.root.findViewById(R.id.bottomPaint))
            },
            onError = {
                // Có thể load ảnh mẫu nếu muốn
                animateBottomBar(binding.root.findViewById(R.id.bottomPaint))
            }
        )
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

        // Set selected tool background
        selectedView.setBackgroundResource(R.drawable.bg_tool_selected)
    }

    private fun setupToolSelectionButtons() {
        // Basic tools
        bottomBinding.llFreeStyle.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.setTool(FreestyleTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
        }

        bottomBinding.llLine.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.setTool(LineTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
        }

        bottomBinding.llEraser.setOnClickListener {
            it.scaleIn()
            currentEraserTool = EraserTool()
            currentEraserTool?.setStrokeWidth(30f)  // Set default eraser size
            binding.paintEditorView.setTool(currentEraserTool!!)
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
        }

        // Shape tools
        bottomBinding.llRectangle.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.setTool(RectangleTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
        }

        bottomBinding.llCircle.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.setTool(CircleTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
        }

        bottomBinding.llArrow.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.setTool(ArrowTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
        }

        bottomBinding.llDashLine.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.setTool(DashLineTool())
            updateToolUI(isBlurTool = false)
            updateToolSelection(it)
        }

        // Special tools
        bottomBinding.llBlur.setOnClickListener {
            it.scaleIn()
            currentBlurTool = BlurTool()
            binding.paintEditorView.setTool(currentBlurTool!!)
            updateToolUI(isBlurTool = true)
            updateToolSelection(it)
        }

        bottomBinding.llColor.setOnClickListener {
            it.scaleIn()
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

        bottomBinding.llClear.setOnClickListener { 
            it.scaleIn()
            binding.paintEditorView.clear() 
        }

        // Setup blur radius seekbar
        binding.seekBarBlurRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBlurTool?.setBlurRadius(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        bottomBinding.llSelect.setOnClickListener {
            it.scaleIn()
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
    }

    private fun updateToolUI(isBlurTool: Boolean) {
        binding.blurRadiusLayout.visibility = if (isBlurTool) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        toolbarBinding.btnFilterList.setOnClickListener {
            it.scaleIn()
            showLineWidthDialog()
        }

        toolbarBinding.btnBlurLinear.setOnClickListener {
            it.scaleIn()
            showOpacityDialog()
        }

        toolbarBinding.btnFormatColorReset.setOnClickListener {
            it.scaleIn()
            val isChecked = !it.isSelected
            it.isSelected = isChecked // Cập nhật lại trạng thái

            val newIconRes = if (isChecked) R.drawable.ic_raindrop else R.drawable.ic_format_color_reset
            toolbarBinding.btnFormatColorReset.setImageResource(newIconRes)

            binding.paintEditorView.setFillMode(isChecked)
        }

        toolbarBinding.btnUndo.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.undo()
        }

        toolbarBinding.btnRedo.setOnClickListener {
            it.scaleIn()
            binding.paintEditorView.redo()
        }

        toolbarBinding.btnCheck.setOnClickListener {
            it.scaleIn()
            val editedBitmap = binding.paintEditorView.getBitmap()
            returnEditedImage(editedBitmap)
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

    override fun onDestroy() {
        super.onDestroy()
        paintSettingsDialog?.dismiss()
        paintSettingsDialog = null
        dialogBinding = null
    }

    override fun onBackPressed() {
        val editedBitmap = binding.paintEditorView.getBitmap()
        returnEditedImage(editedBitmap)
        super.onBackPressed()

    }
}