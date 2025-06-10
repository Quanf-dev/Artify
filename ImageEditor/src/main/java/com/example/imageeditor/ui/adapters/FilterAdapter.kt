package com.example.imageeditor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imageeditor.R
import com.example.imageeditor.utils.FilterPreview

/**
 * Adapter for displaying filter previews in a RecyclerView
 */
class FilterAdapter(
    private val filters: List<FilterPreview>,
    private val onFilterSelected: (FilterPreview) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    // Track selected filter position
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        holder.bind(filter, position == selectedPosition)
        
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            
            // Update UI for previous and newly selected items
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            
            // Notify listener
            onFilterSelected(filter)
        }
    }

    override fun getItemCount(): Int = filters.size

    /**
     * Select a specific filter by position
     */
    fun selectFilter(position: Int) {
        if (position in 0 until itemCount && position != selectedPosition) {
            val previousSelected = selectedPosition
            selectedPosition = position
            
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            
            onFilterSelected(filters[position])
        }
    }

    /**
     * ViewHolder for filter items
     */
    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewFilter)
        private val textView: TextView = itemView.findViewById(R.id.textViewFilterName)

        fun bind(filter: FilterPreview, isSelected: Boolean) {
            imageView.setImageBitmap(filter.previewBitmap)
            textView.text = filter.name
            
            // Highlight selected item
            if (isSelected) {
                itemView.setBackgroundResource(android.R.color.holo_blue_dark)
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
            }
        }
    }
} 