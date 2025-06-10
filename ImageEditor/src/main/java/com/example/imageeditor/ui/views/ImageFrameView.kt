package com.example.imageeditor.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imageeditor.R
import com.example.imageeditor.ui.adapters.FrameAdapter
import com.example.imageeditor.utils.FramePreview
import com.example.imageeditor.utils.FrameTemplateManager

/**
 * View that handles image framing with a horizontal gallery of frame previews
 */
class ImageFrameView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Views
    private lateinit var imageViewMain: ImageView
    private lateinit var recyclerViewFrames: RecyclerView
    private lateinit var textViewFrameName: TextView
    
    // Frame manager
    private val frameManager = FrameTemplateManager(context)
    
    // Frame adapter
    private lateinit var frameAdapter: FrameAdapter
    
    // Available frames
    private lateinit var frames: List<FramePreview>
    
    // Listener for when the image changes
    private var onImageChangedListener: ((Bitmap?) -> Unit)? = null
    
    // Sample bitmap for preview (kept for recreating previews if needed)
    private var sampleBitmap: Bitmap? = null

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_frame_template, this, true)
        
        // Get views
        imageViewMain = findViewById(R.id.imageViewMain)
        recyclerViewFrames = findViewById(R.id.recyclerViewFrames)
        textViewFrameName = findViewById(R.id.textViewFrameName)
        
        // Load default sample bitmap if none is provided
        loadDefaultSampleBitmap()
    }
    
    /**
     * Load a default sample bitmap from resources
     */
    private fun loadDefaultSampleBitmap() {
        if (sampleBitmap == null) {
            // Load a sample image for previews
            try {
                sampleBitmap = BitmapFactory.decodeResource(resources, R.drawable.sample_image)
            } catch (e: Exception) {
                // If sample_image doesn't exist, create a simple colored bitmap
                sampleBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(0xFF888888.toInt())
                }
            }
        }
    }
    
    /**
     * Set the bitmap to be edited
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            // Set the original bitmap
            frameManager.setOriginalBitmap(bitmap)
            
            // Show the image
            imageViewMain.setImageBitmap(bitmap)
            
            // Remember as sample for previews
            sampleBitmap = bitmap
            
            // Get frame previews
            frames = frameManager.getFramePreviews(bitmap)
            
            // Setup frame adapter
            setupFrameAdapter()
        }
    }
    
    /**
     * Set up the frame adapter and RecyclerView
     */
    private fun setupFrameAdapter() {
        frameAdapter = FrameAdapter(frames) { framePreview ->
            // Apply selected frame
            applyFrame(framePreview)
        }
        
        recyclerViewFrames.adapter = frameAdapter
    }
    
    /**
     * Apply a frame to the image
     */
    private fun applyFrame(framePreview: FramePreview) {
        // Update frame name
        textViewFrameName.text = framePreview.template.name
        
        // Apply frame
        val framedBitmap = frameManager.applyFrame(framePreview.template)
        
        // Update image view
        imageViewMain.setImageBitmap(framedBitmap)
        
        // Notify listener
        onImageChangedListener?.invoke(framedBitmap)
    }
    
    /**
     * Reset to no frame
     */
    fun resetFrame() {
        // Select first frame (No Frame)
        frameAdapter.selectFrame(0)
    }
    
    /**
     * Get the current framed bitmap
     */
    fun getCurrentBitmap(): Bitmap? {
        return frameManager.getCurrentBitmap()
    }
    
    /**
     * Set a listener to be called when the image changes
     */
    fun setOnImageChangedListener(listener: (Bitmap?) -> Unit) {
        onImageChangedListener = listener
    }
    
    /**
     * Undo last frame action
     */
    fun undo(): Boolean {
        val previousBitmap = frameManager.undo()
        if (previousBitmap != null) {
            imageViewMain.setImageBitmap(previousBitmap)
            onImageChangedListener?.invoke(previousBitmap)
            return true
        }
        return false
    }
    
    /**
     * Redo last undone frame action
     */
    fun redo(): Boolean {
        val redoBitmap = frameManager.redo()
        if (redoBitmap != null) {
            imageViewMain.setImageBitmap(redoBitmap)
            onImageChangedListener?.invoke(redoBitmap)
            return true
        }
        return false
    }
} 