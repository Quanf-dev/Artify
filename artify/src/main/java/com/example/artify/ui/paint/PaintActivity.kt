package com.example.artify.ui.paint

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
// import android.provider.MediaStore // Not used directly
// import android.widget.Button // No longer needed with ViewBinding
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
// import androidx.appcompat.app.AppCompatActivity // No longer extending AppCompatActivity directly
import com.example.artify.databinding.ActivityImageEditorTestBinding // Import ViewBinding class
import com.example.artify.ui.editbase.BaseEditActivity // Import BaseEditActivity
import com.example.imageeditor.models.BrushStyle
import com.example.imageeditor.tools.EraserTool
import com.example.imageeditor.tools.FreeBrushTool
// import com.example.imageeditor.views.PaintCanvasView // Accessed via binding
import java.io.IOException

class PaintActivity : BaseEditActivity<ActivityImageEditorTestBinding>() { // Inherit from BaseEditActivity

    // binding is now initialized by BaseEditActivity
    // private lateinit var binding: ActivityImageEditorTestBinding // Declare binding variable
    // private lateinit var paintCanvasView: PaintCanvasView // Accessed via binding.paintCanvasViewTest
    private var currentStrokeWidth = 5f

    override fun inflateBinding(): ActivityImageEditorTestBinding {
        return ActivityImageEditorTestBinding.inflate(layoutInflater)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val selectedBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (selectedBitmap != null) {
                    val scaledBitmap = scaleBitmapToView(selectedBitmap, binding.paintCanvasViewTest.width, binding.paintCanvasViewTest.height)
                    val mutableBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    scaledBitmap.recycle()
                    
                    binding.paintCanvasViewTest.getLayerManager()?.addLayerWithBitmap("Image Layer", mutableBitmap)
                    binding.paintCanvasViewTest.getLayerManager()?.addLayer("Drawing Layer")
                    binding.paintCanvasViewTest.invalidate()
                    Toast.makeText(this, "Image loaded as new layer", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @SuppressLint("UseKtx")
    private fun scaleBitmapToView(bitmap: Bitmap, viewWidth: Int, viewHeight: Int): Bitmap {
        if (viewWidth == 0 || viewHeight == 0) return bitmap

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scaleX = viewWidth.toFloat() / originalWidth
        val scaleY = viewHeight.toFloat() / originalHeight

        val scale = Math.max(scaleX, scaleY)

        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        val srcX = (newWidth - viewWidth) / 2
        val srcY = (newHeight - viewHeight) / 2
        
        val targetBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(targetBitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val tempScaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val effectiveSrcX = Math.max(0, srcX)
        val effectiveSrcY = Math.max(0, srcY)
        val drawWidth = Math.min(viewWidth, tempScaledBitmap.width - effectiveSrcX)
        val drawHeight = Math.min(viewHeight, tempScaledBitmap.height - effectiveSrcY)

        val srcRect = android.graphics.Rect(effectiveSrcX, effectiveSrcY, effectiveSrcX + drawWidth, effectiveSrcY + drawHeight)
        val destRect = android.graphics.Rect(0, 0, drawWidth, drawHeight)
        
        canvas.drawBitmap(tempScaledBitmap, srcRect, destRect, null)
        
        if (tempScaledBitmap != bitmap) {
            tempScaledBitmap.recycle()
        }
        
        return targetBitmap
    }

    @SuppressLint("ObsoleteSdkInt")
    // @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM) // API check moved to onUndo/onRedo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Call super.onCreate for BaseEditActivity
        // binding = ActivityImageEditorTestBinding.inflate(layoutInflater) // Done by BaseEditActivity via inflateBinding()
        // setContentView(binding.root) // Done by BaseEditActivity
        binding.ictest.setOnClickListener {
            Toast.makeText(this,"test",Toast.LENGTH_SHORT).show()
        }
        // paintCanvasView is now binding.paintCanvasViewTest
        binding.paintCanvasViewTest.setCurrentBrushStyle(BrushStyle(color = Color.BLACK, strokeWidth = currentStrokeWidth))
        binding.paintCanvasViewTest.setCurrentTool(FreeBrushTool())

        binding.buttonSetRed.setOnClickListener {
            binding.paintCanvasViewTest.setBrushColor(Color.RED)
        }

        binding.buttonSetBlue.setOnClickListener {
            binding.paintCanvasViewTest.setBrushColor(Color.BLUE)
        }

        binding.buttonBrushTool.setOnClickListener { // This button is "Free Brush" now in XML
            binding.paintCanvasViewTest.setCurrentTool(FreeBrushTool())
            val currentStyle = binding.paintCanvasViewTest.getCurrentBrushStyle()
            binding.paintCanvasViewTest.setCurrentBrushStyle(currentStyle.copy(strokeWidth = currentStrokeWidth))
            Toast.makeText(this, "Free Brush Tool Selected", Toast.LENGTH_SHORT).show()
        }

        binding.buttonEraserTool.setOnClickListener {
            binding.paintCanvasViewTest.setCurrentTool(EraserTool())
            binding.paintCanvasViewTest.setStrokeWidth(currentStrokeWidth) // Eraser also uses stroke width for its size
            Toast.makeText(this, "Eraser Tool Selected", Toast.LENGTH_SHORT).show()
        }
        
        binding.buttonIncreaseStroke.setOnClickListener {
            currentStrokeWidth = (currentStrokeWidth + 2f).coerceAtMost(100f)
            binding.paintCanvasViewTest.setStrokeWidth(currentStrokeWidth)
            Toast.makeText(this, "Stroke: $currentStrokeWidth", Toast.LENGTH_SHORT).show()
        }

        binding.buttonDecreaseStroke.setOnClickListener {
            currentStrokeWidth = (currentStrokeWidth - 2f).coerceAtLeast(1f)
            binding.paintCanvasViewTest.setStrokeWidth(currentStrokeWidth)
            Toast.makeText(this, "Stroke: $currentStrokeWidth", Toast.LENGTH_SHORT).show()
        }

        binding.buttonAddLayer.setOnClickListener {
            binding.paintCanvasViewTest.getLayerManager()?.addLayer("Layer ${binding.paintCanvasViewTest.getLayerManager()?.allLayers?.size?.plus(1)}")
            Toast.makeText(this, "Layer Added", Toast.LENGTH_SHORT).show()
        }

        binding.buttonClearLayer.setOnClickListener {
            binding.paintCanvasViewTest.clearCurrentLayer()
            Toast.makeText(this, "Current Layer Cleared", Toast.LENGTH_SHORT).show()
        }

        binding.buttonLoadImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onUndo() {
        super.onUndo() // Optional: if BaseEditActivity has some base undo logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val success = binding.paintCanvasViewTest.undo()
            if (!success) Toast.makeText(this, "Cannot undo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Undo requires API 35+", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onRedo() {
        super.onRedo() // Optional: if BaseEditActivity has some base redo logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val success = binding.paintCanvasViewTest.redo()
            if (!success) Toast.makeText(this, "Cannot redo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Redo requires API 35+", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCheck() {
        super.onCheck()
        // TODO: Implement save or export functionality here, for example:
        // exportDrawing()
        Toast.makeText(this, "Check button clicked (implement save/export)", Toast.LENGTH_LONG).show()
    }
} 