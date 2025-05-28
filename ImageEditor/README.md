# ProPaint Editor / ImageEditor Library

A simple Android library for basic image painting and editing functionalities.

## Features

*   Freehand drawing, shape drawing, eraser.
*   Layer management (add, delete, select, reorder, visibility, opacity).
*   Undo/Redo.
*   Zoom and Pan.
*   Customizable brush color and size.
*   Image export (PNG, JPG).
*   Project save/load (JSON format).

## Integration

1.  Add the library module to your project.
    ```gradle
    // In settings.gradle
    // include ':ImageEditor' // if it's a local module
    
    // In your app's build.gradle
    // implementation project(':ImageEditor') 
    // or the relevant maven coordinates if published
    ```

2.  Add `PaintCanvasView` to your layout XML:
    ```xml
    <com.example.imageeditor.views.PaintCanvasView
        android:id="@+id/paintCanvasView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    ```

## Basic Usage

```kotlin
// In your Activity or Fragment

val paintCanvasView = findViewById<PaintCanvasView>(R.id.paintCanvasView)

// Set brush color
paintCanvasView.setBrushColor(Color.RED)

// Set stroke width
paintCanvasView.setStrokeWidth(10f)

// Select a tool (assuming you have tool instances)
// val freeBrushTool = FreeBrushTool()
// paintCanvasView.setCurrentTool(freeBrushTool)

// Undo
paintCanvasView.undo()

// Redo
paintCanvasView.redo()

// Get LayerManager
val layerManager = paintCanvasView.getLayerManager()
layerManager?.addLayer("New Layer")

// Export
// val exportManager = ExportManager(context)
// val bitmapToExport = paintCanvasView.getBitmap()
// if (bitmapToExport != null) {
//     val file = File(getExternalFilesDir(null), "my_drawing.png")
//     lifecycleScope.launch { // if in a coroutine scope
//         val success = exportManager.saveBitmapToFile(bitmapToExport, file)
//         if (success) {
//             // show success message
//         }
//         bitmapToExport.recycle() // Important: recycle the bitmap from getBitmap()
//    }
// }
```

## Further Development

*   Implement UI components (ColorPicker, BrushSizeSlider, ToolBar, LayerList).
*   Advanced drawing tools and effects.
*   More robust project save/load (e.g., saving bitmaps to separate files).
*   Performance enhancements for complex drawings.
*   Detailed documentation and sample application. 