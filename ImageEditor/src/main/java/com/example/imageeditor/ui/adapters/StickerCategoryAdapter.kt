package com.example.imageeditor.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imageeditor.R
import com.example.imageeditor.utils.StickerCategory

/**
 * Adapter for displaying sticker categories in a RecyclerView
 */
class StickerCategoryAdapter(
    private val context: Context,
    private val categories: List<StickerCategory>,
    private val onCategorySelected: (Int) -> Unit
) : RecyclerView.Adapter<StickerCategoryAdapter.CategoryViewHolder>() {

    // Track selected category position
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, position == selectedPosition)
        
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            
            // Update UI for previous and newly selected items
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            
            // Notify listener
            onCategorySelected(selectedPosition)
        }
    }

    override fun getItemCount(): Int = categories.size

    /**
     * ViewHolder for category items
     */
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewCategoryIcon)
        private val textViewName: TextView = itemView.findViewById(R.id.textViewCategoryName)

        fun bind(category: StickerCategory, isSelected: Boolean) {
            try {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 48f
                    color = android.graphics.Color.BLACK
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                val bounds = android.graphics.Rect()
                paint.getTextBounds(category.iconPath, 0, category.iconPath.length, bounds)

                val width = if (bounds.width() <= 0) 1 else bounds.width()
                val height = if (bounds.height() <= 0) 1 else bounds.height()

                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawText(category.iconPath, 0f, -bounds.top.toFloat(), paint)
                
                imageViewIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageViewIcon.setImageResource(R.drawable.ic_sticker_category_placeholder)
            }
            
            textViewName.text = category.name
            
            // Highlight selected item
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_selected_category)
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
            }
        }
    }
} 