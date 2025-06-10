# Image Filter Library for Artify

This module provides image filtering capabilities with a horizontal filter preview gallery.

## Features

- Horizontal gallery of filter previews
- 16+ predefined filters with real-time preview
- Extensible architecture for adding custom filters
- Undo/redo functionality
- Support for saving filtered images
- Integration with camera
- Easily extendable for future camera integrations

## Usage

### Basic Implementation

1. Add the `ImageFilterView` to your layout:

```xml
<com.example.imageeditor.ui.views.ImageFilterView
    android:id="@+id/imageFilterView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

2. Set an image to filter:

```kotlin
// In your activity or fragment
val imageFilterView = findViewById(R.id.imageFilterView)
imageFilterView.setImageBitmap(yourBitmap)
```

3. Get the filtered image:

```kotlin
val filteredBitmap = imageFilterView.getCurrentBitmap()
```

### Advanced Features

#### Listen for Image Changes

```kotlin
imageFilterView.setOnImageChangedListener { bitmap ->
    // Do something with the filtered bitmap
}
```

#### Reset Filters

```kotlin
imageFilterView.resetFilters()
```

#### Undo/Redo

```kotlin
// Undo last filter action
val undoSuccess = imageFilterView.undo()

// Redo last undone filter action
val redoSuccess = imageFilterView.redo()
```

## Available Filters

- Normal (no filter)
- Sepia
- Grayscale
- Invert
- Vintage
- Sweet
- Cool
- Warm
- Sketch
- Toon
- Posterize
- Pixelate
- High Contrast
- Vibrant
- Muted
- Dreamy

## Extending with Custom Filters

You can add custom filters by extending the `ImageFilterManager` class and creating your own GPUImage filters.

Example:

```kotlin
// Create a custom filter
private fun createCustomFilter(): GPUImageFilter {
    val filter = GPUImageFilterGroup()
    filter.addFilter(GPUImageContrastFilter(1.5f))
    filter.addFilter(GPUImageSaturationFilter(0.8f))
    filter.addFilter(GPUImageBrightnessFilter(0.1f))
    return filter
}
```

## Integration with Camera

The test activity in the Artify app demonstrates how to integrate with the device camera and gallery. Use it as a reference for integrating the filter module with camera functionality. 