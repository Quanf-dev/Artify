package com.example.artify.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.R
import com.example.imageeditor.views.PaintCanvasView
import com.example.imageeditor.tools.FreeBrushTool
import com.example.imageeditor.tools.EraserTool
import com.example.imageeditor.models.BrushStyle
import android.widget.Toast
import androidx.annotation.RequiresApi

class ImageEditorTestActivity : AppCompatActivity() {

    private lateinit var paintCanvasView: PaintCanvasView
    private var currentStrokeWidth = 5f

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_editor_test)

        paintCanvasView = findViewById(R.id.paintCanvasViewTest)

        // Setup initial brush (optional, PaintCanvasView has a default)
        paintCanvasView.setCurrentBrushStyle(BrushStyle(color = Color.BLACK, strokeWidth = currentStrokeWidth))
        paintCanvasView.setCurrentTool(FreeBrushTool())

        findViewById<Button>(R.id.buttonSetRed).setOnClickListener {
            paintCanvasView.setBrushColor(Color.RED)
        }

        findViewById<Button>(R.id.buttonSetBlue).setOnClickListener {
            paintCanvasView.setBrushColor(Color.BLUE)
        }

        findViewById<Button>(R.id.buttonUndo).setOnClickListener {
            val success = paintCanvasView.undo()
            if (!success) Toast.makeText(this, "Cannot undo", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonRedo).setOnClickListener {
            val success = paintCanvasView.redo()
            if (!success) Toast.makeText(this, "Cannot redo", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonBrushTool).setOnClickListener {
            paintCanvasView.setCurrentTool(FreeBrushTool())
            // Re-apply current color and stroke to new tool instance
            val currentStyle = paintCanvasView.getCurrentBrushStyle()
            paintCanvasView.setCurrentBrushStyle(currentStyle.copy(strokeWidth = currentStrokeWidth))
             Toast.makeText(this, "Brush Tool Selected", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonEraserTool).setOnClickListener {
            paintCanvasView.setCurrentTool(EraserTool())
            // EraserTool might have its own default size, or you can set it
            // Forcing current stroke width for eraser for consistency in this test UI
             paintCanvasView.setStrokeWidth(currentStrokeWidth) // EraserTool will use this for its effect
            Toast.makeText(this, "Eraser Tool Selected", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.buttonIncreaseStroke).setOnClickListener {
            currentStrokeWidth = (currentStrokeWidth + 2f).coerceAtMost(100f)
            paintCanvasView.setStrokeWidth(currentStrokeWidth)
            Toast.makeText(this, "Stroke: $currentStrokeWidth", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonDecreaseStroke).setOnClickListener {
            currentStrokeWidth = (currentStrokeWidth - 2f).coerceAtLeast(1f)
            paintCanvasView.setStrokeWidth(currentStrokeWidth)
            Toast.makeText(this, "Stroke: $currentStrokeWidth", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonAddLayer).setOnClickListener {
            paintCanvasView.getLayerManager()?.addLayer("Layer ${paintCanvasView.getLayerManager()?.allLayers?.size?.plus(1)}")
            Toast.makeText(this, "Layer Added", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonClearLayer).setOnClickListener {
            paintCanvasView.clearCurrentLayer()
            Toast.makeText(this, "Current Layer Cleared", Toast.LENGTH_SHORT).show()
        }
    }
} 