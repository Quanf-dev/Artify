package com.example.imageeditor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imageeditor.R
import com.example.imageeditor.utils.FramePreview

/**
 * Adapter for displaying frame previews in a RecyclerView
 */
class FrameAdapter(
    private val frames: List<FramePreview>,
    private val onFrameSelected: (FramePreview) -> Unit
) : RecyclerView.Adapter<FrameAdapter.FrameViewHolder>() {

    // Track selected frame position
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frame, parent, false)
        return FrameViewHolder(view)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        val frame = frames[position]
        holder.bind(frame, position == selectedPosition)
        
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            
            // Update UI for previous and newly selected items
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            
            // Notify listener
            onFrameSelected(frame)
        }
    }

    override fun getItemCount(): Int = frames.size

    /**
     * Select a specific frame by position
     */
    fun selectFrame(position: Int) {
        if (position in 0 until itemCount && position != selectedPosition) {
            val previousSelected = selectedPosition
            selectedPosition = position
            
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            
            onFrameSelected(frames[position])
        }
    }

    /**
     * ViewHolder for frame items
     */
    inner class FrameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewFrame)
        private val textView: TextView = itemView.findViewById(R.id.textViewFrameName)

        fun bind(frame: FramePreview, isSelected: Boolean) {
            imageView.setImageBitmap(frame.previewBitmap)
            textView.text = frame.template.name
            
            // Highlight selected item
            if (isSelected) {
                itemView.setBackgroundResource(android.R.color.holo_blue_dark)
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
            }
        }
    }
} 