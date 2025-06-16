package com.example.camera.ui.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.camera.R
import java.io.File

class ImagePagerAdapter(
    private val context: Context,
    private val imagePaths: List<String>,
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    private val TAG = "ImagePagerAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = imagePaths[position]
        val imageFile = File(imagePath)
        

        if (imageFile.exists()) {
            Log.d(TAG, "Image file exists, size: ${imageFile.length()} bytes")
            
            try {
                // Load the original bitmap
                val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                
                if (originalBitmap != null) {
                    // Apply color filter to the bitmap (images are already saved with filters, but this ensures consistency)
                    // Since images should already have the filter applied during capture, we'll display them as-is
                    // But we can apply filter here if needed for real-time preview adjustment
                    holder.zoomableImageView.setImageBitmap(originalBitmap)
                    holder.zoomableImageView.setScaleFactor(1.0f) // Reset zoom for new image
                    
                } else {
                    Log.e(TAG, "Failed to decode bitmap from file: $imagePath")
                    holder.zoomableImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: ${e.message}", e)
                holder.zoomableImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            Log.e(TAG, "Image file does not exist: $imagePath")
            // Set a placeholder or error image if needed
            holder.zoomableImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount(): Int = imagePaths.size

    class ImageViewHolder(itemView: View, context: Context) : RecyclerView.ViewHolder(itemView) {
        val zoomableImageView: ZoomableImageView = itemView.findViewById(R.id.preview_image_item)
        
        init {
            Log.d("ImageViewHolder", "ImageViewHolder created with ZoomableImageView")
        }
    }
} 