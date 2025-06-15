package com.example.camera.ui.preview

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.camera.R
import java.io.File

class ImagePagerAdapter(
    private val context: Context,
    private val imagePaths: List<String>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = imagePaths[position]
        val imageFile = File(imagePath)
        
        if (imageFile.exists()) {
            holder.imageView.setImageURI(Uri.fromFile(imageFile))
        }
        
        // Reset zoom when binding a new item
        holder.resetZoom()
    }

    override fun getItemCount(): Int = imagePaths.size

    class ImageViewHolder(itemView: View, context: Context) : RecyclerView.ViewHolder(itemView) {
        val imageView: ZoomableImageView = itemView.findViewById(R.id.preview_image_item)
        
        private val scaleGestureDetector: ScaleGestureDetector
        private var scaleFactor = 1.0f
        private val minZoom = 1.0f
        private val maxZoom = 5.0f
        
        init {
            // Initialize scale gesture detector
            scaleGestureDetector = ScaleGestureDetector(context, 
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        scaleFactor *= detector.scaleFactor
                        scaleFactor = scaleFactor.coerceIn(minZoom, maxZoom)
                        imageView.setScaleFactor(scaleFactor)
                        return true
                    }
                }
            )
            
            // Set touch listener for zoom
            imageView.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                true
            }
        }
        
        fun resetZoom() {
            scaleFactor = 1.0f
            imageView.setScaleFactor(scaleFactor)
        }
    }
} 