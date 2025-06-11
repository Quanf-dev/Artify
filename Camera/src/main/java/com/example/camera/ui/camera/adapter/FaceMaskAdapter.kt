package com.example.camera.ui.camera.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.camera.databinding.ItemFaceMaskBinding
import com.example.camera.domain.model.FaceMask

class FaceMaskAdapter(
    private val onMaskSelected: (FaceMask?) -> Unit
) : ListAdapter<FaceMask, FaceMaskAdapter.FaceMaskViewHolder>(FaceMaskDiffCallback()) {

    private var selectedMask: FaceMask? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceMaskViewHolder {
        val binding = ItemFaceMaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FaceMaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaceMaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setSelectedMask(mask: FaceMask?) {
        val oldSelectedIndex = currentList.indexOf(selectedMask)
        val newSelectedIndex = currentList.indexOf(mask)
        
        selectedMask = mask
        
        if (oldSelectedIndex != -1) {
            notifyItemChanged(oldSelectedIndex)
        }
        if (newSelectedIndex != -1) {
            notifyItemChanged(newSelectedIndex)
        }
    }

    inner class FaceMaskViewHolder(
        private val binding: ItemFaceMaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mask: FaceMask) {
            binding.apply {
                tvMaskName.text = mask.name
                ivMaskPreview.setImageResource(mask.previewImage)
                
                // Set selected state
                root.isSelected = mask == selectedMask
                
                root.setOnClickListener {
                    val newSelectedMask = if (mask == selectedMask) null else mask
                    onMaskSelected(newSelectedMask)
                    setSelectedMask(newSelectedMask)
                }
            }
        }
    }

    private class FaceMaskDiffCallback : DiffUtil.ItemCallback<FaceMask>() {
        override fun areItemsTheSame(oldItem: FaceMask, newItem: FaceMask): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FaceMask, newItem: FaceMask): Boolean {
            return oldItem == newItem
        }
    }
} 