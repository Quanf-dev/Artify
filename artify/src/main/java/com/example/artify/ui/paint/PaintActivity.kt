package com.example.artify.ui.paint

import android.R
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.artify.databinding.ActivityPaintBinding
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
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.graphics.drawable.BitmapDrawable

class PaintActivity : BaseEditActivity<ActivityPaintBinding>() {
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var currentSelectedColor: Int = Color.BLACK
    private var currentBlurTool: BlurTool? = null

    override fun inflateBinding(): ActivityPaintBinding {
        return ActivityPaintBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupImagePicker()
        setupPaintEditorView()
        setupToolSelectionButtons()
        setupColorPicker()
        setupSeekBars()
        setupActionButtons()
        setupUtilityButtons()
        setupFillModeSwitch()
        setupRotationSeekBar()

        // Ưu tiên nhận image_path từ Intent
        val imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                binding.paintEditorView.setBackgroundImage(bitmap)
            }
        } else {
            // Fallback: lấy từ sharedImageView nếu không có image_path
            val drawable = sharedImageView.drawable
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                val bitmap = drawable.bitmap
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
        binding.viewSelectedColorPreview.setBackgroundColor(currentSelectedColor)
    }

    private fun setupToolSelectionButtons() {
        // Basic tools
        binding.btnFreestyleTool.setOnClickListener {
            binding.paintEditorView.setTool(FreestyleTool())
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Freestyle Tool Selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnLineTool.setOnClickListener {
            binding.paintEditorView.setTool(LineTool())
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Line Tool Selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnEraserTool.setOnClickListener {
            binding.paintEditorView.setTool(EraserTool())
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Eraser Tool Selected", Toast.LENGTH_SHORT).show()
        }

        // Shape tools
        binding.btnRectangleTool.setOnClickListener {
            binding.paintEditorView.setTool(RectangleTool())
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Rectangle Tool Selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnCircleTool.setOnClickListener {
            binding.paintEditorView.setTool(CircleTool())
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Circle Tool Selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnArrowTool.setOnClickListener {
            binding.paintEditorView.setTool(ArrowTool())
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Arrow Tool Selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnDashLineTool.setOnClickListener {
            binding.paintEditorView.setTool(DashLineTool())
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Dash Line Tool Selected", Toast.LENGTH_SHORT).show()
        }

        // Special tools
        binding.btnBlurTool.setOnClickListener {
            currentBlurTool = BlurTool()
            binding.paintEditorView.setTool(currentBlurTool!!)
            updateToolUI(isBlurTool = true)
            Toast.makeText(this, "Blur Tool Selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnSelectionTool.setOnClickListener {
            val selectionTool = SelectionTool(
                binding.paintEditorView.getLayerManager(),
                view = binding.paintEditorView
            ).apply {
                setOnItemSelectedListener { item ->
                    // Show rotation controls when an item is selected
                    binding.rotationLayout.visibility = if (item != null) View.VISIBLE else View.GONE
                    // Update rotation seekbar to match selected item's rotation
                    item?.let {
                        binding.seekBarRotation.progress = it.rotation.toInt()
                    }
                }
            }
            binding.paintEditorView.setTool(selectionTool)
            updateToolUI(isBlurTool = false)
            Toast.makeText(this, "Selection Tool Selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToolUI(isBlurTool: Boolean) {
        binding.blurRadiusLayout.visibility = if (isBlurTool) View.VISIBLE else View.GONE
        // Show rotation controls only when an item is selected
        binding.rotationLayout.visibility = if ((binding.paintEditorView.currentTool as? SelectionTool)?.getSelectedItem() != null) View.VISIBLE else View.GONE
    }

    private fun setupColorPicker() {
        binding.viewSelectedColorPreview.setBackgroundColor(currentSelectedColor)
        binding.btnChooseColor.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("Choose Color")
                .setPreferenceName("ImageEditorTestActivityColorPicker")
                .setPositiveButton(getString(R.string.ok),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                            envelope?.let {
                                currentSelectedColor = it.color
                                binding.viewSelectedColorPreview.setBackgroundColor(currentSelectedColor)
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
    }

    private fun setupSeekBars() {
        binding.seekBarStrokeWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.paintEditorView.setStrokeWidth(progress.toFloat().coerceAtLeast(1f))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.seekBarStrokeWidth.progress = 5

        binding.seekBarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.paintEditorView.setOpacity(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.seekBarOpacity.progress = 255

        binding.seekBarBlurRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBlurTool?.setBlurRadius(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupFillModeSwitch() {
        binding.switchFillMode.setOnCheckedChangeListener { _, isChecked ->
            binding.paintEditorView.setFillMode(isChecked)
        }
    }

    private fun setupActionButtons() {
        binding.btnClear.setOnClickListener { binding.paintEditorView.clear() }
    }

    private fun setupUtilityButtons() {
        binding.btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }
        binding.btnResetViewTransform.setOnClickListener { binding.paintEditorView.resetViewTransformations() }
    }

    private fun saveDrawing() {
        try {
            val bitmap = binding.paintEditorView.getBitmap()
            if (bitmap == null) {
                Toast.makeText(this, "Canvas is empty or not ready.", Toast.LENGTH_SHORT).show()
                return
            }
            val displayName = "Artify_Drawing_${System.currentTimeMillis()}.png"
            val file = File(getExternalFilesDir(null), displayName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                bitmap.recycle()
            }
            Toast.makeText(this, "Drawing saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save drawing: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun setupRotationSeekBar() {
        binding.seekBarRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                (binding.paintEditorView.currentTool as? SelectionTool)?.getSelectedItem()?.let { item ->
                    item.rotation = progress.toFloat()
                    binding.paintEditorView.redrawAllLayers()
                    binding.paintEditorView.invalidate()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}