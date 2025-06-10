package com.example.imageeditor.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imageeditor.R
import com.example.imageeditor.ui.adapters.FilterAdapter
import com.example.imageeditor.utils.FilterPreview
import com.example.imageeditor.utils.ImageFilterManager

/**
 * View that handles image filtering with a horizontal gallery of filter previews
 */
class ImageFilterView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Views
    private lateinit var imageViewMain: ImageView
    private lateinit var recyclerViewFilters: RecyclerView
    private lateinit var textViewFilterName: TextView
    
    // Filter manager
    private val filterManager = ImageFilterManager(context)
    
    // Filter adapter
    private lateinit var filterAdapter: FilterAdapter
    
    // Available filters
    private lateinit var filters: List<FilterPreview>
    
    // Listener for when the image changes
    private var onImageChangedListener: ((Bitmap?) -> Unit)? = null

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_filter_gallery, this, true)
        
        // Get views
        imageViewMain = findViewById(R.id.imageViewMain)
        recyclerViewFilters = findViewById(R.id.recyclerViewFilters)
        textViewFilterName = findViewById(R.id.textViewFilterName)
    }
    
    /**
     * Set the bitmap to be edited
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            // Set the original bitmap
            filterManager.setOriginalBitmap(bitmap)
            
            // Show the image
            imageViewMain.setImageBitmap(bitmap)
            
            // Get filter previews
            filters = filterManager.getFilterPreviews()
            
            // Setup filter adapter
            setupFilterAdapter()
        }
    }
    
    /**
     * Set up the filter adapter and RecyclerView
     */
    private fun setupFilterAdapter() {
        filterAdapter = FilterAdapter(filters) { filterPreview ->
            // Apply selected filter
            applyFilter(filterPreview)
        }
        
        recyclerViewFilters.adapter = filterAdapter
    }
    
    /**
     * Apply a filter to the image
     */
    private fun applyFilter(filterPreview: FilterPreview) {
        // Update filter name
        textViewFilterName.text = filterPreview.name
        
        // Apply filter
        val filteredBitmap = filterManager.applyFilter(filterPreview.filter)
        
        // Update image view
        imageViewMain.setImageBitmap(filteredBitmap)
        
        // Notify listener
        onImageChangedListener?.invoke(filteredBitmap)
    }
    
    /**
     * Reset all filters
     */
    fun resetFilters() {
        val resetBitmap = filterManager.resetFilters()
        imageViewMain.setImageBitmap(resetBitmap)
        textViewFilterName.text = "Normal"
        
        // Select first filter (Normal)
        filterAdapter.selectFilter(0)
        
        onImageChangedListener?.invoke(resetBitmap)
    }
    
    /**
     * Get the current filtered bitmap
     */
    fun getCurrentBitmap(): Bitmap? {
        return filterManager.getCurrentBitmap()
    }
    
    /**
     * Set a listener to be called when the image changes
     */
    fun setOnImageChangedListener(listener: (Bitmap?) -> Unit) {
        onImageChangedListener = listener
    }
    
    /**
     * Undo last filter action
     */
    fun undo(): Boolean {
        val previousBitmap = filterManager.undo()
        if (previousBitmap != null) {
            imageViewMain.setImageBitmap(previousBitmap)
            onImageChangedListener?.invoke(previousBitmap)
            return true
        }
        return false
    }
    
    /**
     * Redo last undone filter action
     */
    fun redo(): Boolean {
        val redoBitmap = filterManager.redo()
        if (redoBitmap != null) {
            imageViewMain.setImageBitmap(redoBitmap)
            onImageChangedListener?.invoke(redoBitmap)
            return true
        }
        return false
    }
} 