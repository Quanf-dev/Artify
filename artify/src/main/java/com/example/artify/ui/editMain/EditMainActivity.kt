package com.example.artify.ui.editMain

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.artify.R
import com.example.imageeditor.tools.text.TextTool
import com.example.imageeditor.tools.text.TextSelectionTool
import com.example.imageeditor.ui.views.PaintEditorView
import java.io.File
import java.io.FileOutputStream

class EditMainActivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private lateinit var editorView: PaintEditorView
    private var currentTextTool: TextTool? = null
    private var currentTextSelectionTool: TextSelectionTool? = null
    private var textEditDialog: Dialog? = null
    private var currentTextColor = Color.BLACK
    private var currentTextBgColor = Color.WHITE
    private var currentTextBgAlpha = 128 // 50% opacity

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val inputStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                // Lưu bitmap vào file tạm
                val file = File(cacheDir, "editing_image.png")
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                imageUri = Uri.fromFile(file)
                editorView.setBackgroundImage(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        editorView = findViewById(R.id.editorView)
        setupTextEditDialog()
        setupTextSelectionTool()

        // Gắn sự kiện cho nút lltext để mở TextOnImage
        findViewById<View>(R.id.llText)?.setOnClickListener {
            imageUri?.let { uri ->
                // Create a new TextTool instance
                currentTextTool = TextTool()
                currentTextTool?.setText("Nhập văn bản") // Default text
                currentTextTool?.setTextSize(50f) // Default text size
                currentTextTool?.setColor(currentTextColor) // Default text color
                currentTextTool?.setBackgroundColor(currentTextBgColor)
                currentTextTool?.setBackgroundAlpha(currentTextBgAlpha)
                
                // Set the text tool as the current tool
                currentTextTool?.let { tool ->
                    editorView.setTool(tool)
                }
            }
        }
    }

    private fun setupTextEditDialog() {
        textEditDialog = Dialog(this).apply {
            setContentView(R.layout.dialog_text_edit)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Setup dialog views
            val etText = findViewById<EditText>(R.id.etText)
            val spinnerFont = findViewById<Spinner>(R.id.spinnerFont)
            val seekBarTextSize = findViewById<SeekBar>(R.id.seekBarTextSize)
            val radioGroupAlign = findViewById<RadioGroup>(R.id.radioGroupAlign)
            val viewTextColor = findViewById<View>(R.id.viewTextColor)
            val viewBgColor = findViewById<View>(R.id.viewBgColor)
            val seekBarBgOpacity = findViewById<SeekBar>(R.id.seekBarBgOpacity)

            // Set initial values
            etText.setText("Nhập văn bản")
            seekBarTextSize.progress = 50 // Default text size
            radioGroupAlign.check(R.id.rbCenter) // Default alignment
            viewTextColor.setBackgroundColor(currentTextColor)
            viewBgColor.setBackgroundColor(currentTextBgColor)
            seekBarBgOpacity.progress = currentTextBgAlpha

            // Setup listeners
            etText.setOnEditorActionListener { _, _, _ ->
                currentTextTool?.setText(etText.text.toString())
                editorView.redrawAllLayers()
                true
            }

            seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val textSize = 20f + (progress * 2f) // Scale from 20 to 220
                    currentTextTool?.setTextSize(textSize)
                    editorView.redrawAllLayers()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            radioGroupAlign.setOnCheckedChangeListener { _, checkedId ->
                val align = when (checkedId) {
                    R.id.rbLeft -> Paint.Align.LEFT
                    R.id.rbCenter -> Paint.Align.CENTER
                    R.id.rbRight -> Paint.Align.RIGHT
                    else -> Paint.Align.LEFT
                }
                currentTextTool?.setTextAlign(align)
                editorView.redrawAllLayers()
            }
        }
    }

    private fun setupTextSelectionTool() {
        currentTextSelectionTool = TextSelectionTool(
            editorView.getLayerManager(),
            view = editorView
        )
        
        currentTextSelectionTool?.setOnTextSelectedListener { textDrawable ->
            // Update UI when text is selected
        }
        
        currentTextSelectionTool?.setOnTextEditRequestListener { textDrawable ->
            // Show edit dialog with current text properties
            textEditDialog?.show()
        }
        
        currentTextSelectionTool?.setOnTextDeleteRequestListener { textDrawable ->
            // Remove text from layer
            editorView.getLayerManager().getActiveLayer()?.removeDrawable(textDrawable)
            editorView.redrawAllLayers()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == 1 && data != null) {
            val resultImageUri = Uri.parse(data.getStringExtra("imageOutURI"))
            // Convert URI to Bitmap and set as background
            val inputStream = contentResolver.openInputStream(resultImageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let {
                editorView.setBackgroundImage(it)
            }
        }
    }

    // Hàm này có thể gọi từ nút chọn ảnh
    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }
}