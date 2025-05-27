package com.example.artify.ui.profile

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.artify.R

class AvatarAdapter(
    private val context: Context,
    private val avatarUrls: List<String>,
    private val onAvatarSelected: (String) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    private var selectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_avatar, parent, false)
        val imageView = itemView.findViewById<ImageView>(R.id.avatarImageViewItem)
        return ViewHolder(imageView, itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url: String = avatarUrls[position]
        
        // Load avatar image
        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .circleCrop()
            .into(holder.avatarImageView)

        // Set selection state
        updateSelectionState(holder, position)

        // Handle click
        holder.itemView.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                onAvatarSelected(avatarUrls[selectedPosition])
                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)
            }
        }
    }

    private fun updateSelectionState(holder: ViewHolder, position: Int) {
        if (selectedPosition == position) {
            // Create selected border
            val borderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
//                setStroke(8, ContextCompat.getColor(context, R.color.purple_500)) // Use your app's primary color
            }
            holder.avatarImageView.background = borderDrawable
        } else {
            holder.avatarImageView.background = null
        }
    }

    override fun getItemCount(): Int = avatarUrls.size

    fun setSelectedUrl(url: String?) {
        url?.let {
            val index = avatarUrls.indexOf(it)
            if (index != -1 && selectedPosition != index) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = index
                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)
                onAvatarSelected(avatarUrls[selectedPosition])
            }
        }
    }

    class ViewHolder(val avatarImageView: ImageView, itemView: android.view.View) : RecyclerView.ViewHolder(itemView)
} 