package com.example.imageeditor.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.imageeditor.R
import com.example.imageeditor.utils.Sticker
import com.example.imageeditor.utils.StickerManager

/**
 * Adapter for displaying stickers in a RecyclerView
 */
class StickerAdapter(
    private val context: Context,
    private var stickers: List<Sticker>,
    private val stickerManager: StickerManager,
    private val onStickerSelected: (Sticker) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker, parent, false)
        return StickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val sticker = stickers[position]
        holder.bind(sticker)
        
        holder.itemView.setOnClickListener {
            onStickerSelected(sticker)
        }
    }

    override fun getItemCount(): Int = stickers.size

    /**
     * Update stickers list
     */
    fun updateStickers(newStickers: List<Sticker>) {
        stickers = newStickers
        notifyDataSetChanged()
    }

    /**
     * ViewHolder for sticker items
     */
    inner class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewSticker: ImageView = itemView.findViewById(R.id.imageViewSticker)

        fun bind(sticker: Sticker) {
            try {
                val drawable = stickerManager.loadStickerDrawable(sticker)
                if (drawable != null) {
                    imageViewSticker.setImageDrawable(drawable)
                } else {
                    imageViewSticker.setImageResource(R.drawable.ic_sticker_placeholder)
                }
            } catch (e: Exception) {
                imageViewSticker.setImageResource(R.drawable.ic_sticker_placeholder)
            }
        }
    }
} 