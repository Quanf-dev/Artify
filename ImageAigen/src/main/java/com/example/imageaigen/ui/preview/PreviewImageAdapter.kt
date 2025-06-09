package com.example.imageaigen.ui.preview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.imageaigen.databinding.ItemPreviewImageBinding

class PreviewImageAdapter(
    private val onImageClick: (Int) -> Unit,
    private val onDownloadClick: (Int) -> Unit,
    private val onShareClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit
) : ListAdapter<PreviewImageItem, PreviewImageAdapter.ViewHolder>(PreviewImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPreviewImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPreviewImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onImageClick(position)
                }
            }
            binding.downloadButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDownloadClick(position)
                }
            }
            binding.shareButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onShareClick(position)
                }
            }
            binding.editButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(position)
                }
            }
        }

        fun bind(item: PreviewImageItem) {
            binding.previewImageView.setImageBitmap(item.bitmap)
        }
    }

    private class PreviewImageDiffCallback : DiffUtil.ItemCallback<PreviewImageItem>() {
        override fun areItemsTheSame(oldItem: PreviewImageItem, newItem: PreviewImageItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PreviewImageItem, newItem: PreviewImageItem): Boolean {
            return oldItem.bitmap.sameAs(newItem.bitmap)
        }
    }
}

data class PreviewImageItem(
    val id: String,
    val bitmap: android.graphics.Bitmap
) 