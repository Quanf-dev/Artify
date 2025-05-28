package com.example.artify.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.artify.R
import com.facebook.shimmer.ShimmerFrameLayout

class AvatarAdapter(
    private val context: Context,
    private val avatarUrls: List<String>,
    private val onAvatarSelected: (String) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private val TAG = "AvatarAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_avatar, parent, false)
        return ViewHolder(itemView)
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = avatarUrls[position]
        Log.d(TAG, "onBindViewHolder at position $position, URL: $url")

        holder.shimmerContainer.visibility = View.VISIBLE
        holder.shimmerContainer.startShimmer()
        holder.avatarImageView.visibility = View.INVISIBLE 

        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_launcher_background) 
            .error(R.drawable.ic_launcher_foreground)
            .circleCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e(TAG, "Glide onLoadFailed for URL: $url at position $position", e)
                    if (holder.bindingAdapterPosition == position) {
                        holder.shimmerContainer.stopShimmer()
                        holder.shimmerContainer.visibility = View.GONE
                        holder.avatarImageView.visibility = View.VISIBLE 
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "Glide onResourceReady for URL: $url at position $position")
                    if (holder.bindingAdapterPosition == position) {
                        holder.shimmerContainer.stopShimmer()
                        holder.shimmerContainer.visibility = View.GONE
                        holder.avatarImageView.visibility = View.VISIBLE
                    }
                    return false
                }
            })
            .into(holder.avatarImageView)

        updateSelectionState(holder, position)

        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = currentPosition
                onAvatarSelected(avatarUrls[selectedPosition])
                if (previousSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousSelectedPosition)
                }
                notifyItemChanged(selectedPosition)
            }
        }
    }

    private fun updateSelectionState(holder: ViewHolder, position: Int) {
        if (selectedPosition == position) {
            val borderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(8, ContextCompat.getColor(context, android.R.color.darker_gray))
            }
            holder.avatarImageView.background = borderDrawable
        } else {
            holder.avatarImageView.background = null
        }
    }

    override fun getItemCount(): Int = avatarUrls.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageViewItem)
        val shimmerContainer: ShimmerFrameLayout = itemView.findViewById(R.id.shimmerViewContainer)
    }
} 