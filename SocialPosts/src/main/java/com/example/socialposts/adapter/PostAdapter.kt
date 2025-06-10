package com.example.socialposts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.socialposts.databinding.ItemPostBinding
import com.example.socialposts.model.Post

class PostAdapter(
    private val onLikeClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnLike.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLikeClick(getItem(position))
                }
            }
        }

        fun bind(post: Post) {
            binding.apply {
                tvUsername.text = post.username
                tvCaption.text = post.caption
                tvTimestamp.text = post.getFormattedTimestamp()
                tvLikeCount.text = "${post.likedBy.size} likes"

                // Load post image
                Glide.with(ivPostImage)
                    .load(post.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(ivPostImage)

                // Load user avatar
                Glide.with(ivUserAvatar)
                    .load(post.avatarUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .circleCrop()
                    .into(ivUserAvatar)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
} 