package com.example.socialposts.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.common.base.BaseActivity
import com.example.socialposts.adapter.PostAdapter
import com.example.socialposts.databinding.ActivityPostsBinding
import com.example.socialposts.viewmodel.PostViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostsActivity : BaseActivity<ActivityPostsBinding>() {

    private val viewModel: PostViewModel by viewModels()
    private lateinit var adapter: PostAdapter

    override fun inflateBinding(): ActivityPostsBinding {
        return ActivityPostsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter { post ->
            viewModel.toggleLikePost(post.id)
        }

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(this@PostsActivity)
            adapter = this@PostsActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.btnCreatePost.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }
    }

    private fun setupBottomNavigation() {
        // Apply animation to bottom navigation appearance
        MainBottomNavigationHelper.animateBottomNavigation(binding.bottomNavigation)
        
        binding.bottomNavigation.apply {
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    binding.bottomNavigation.menu.getItem(0).itemId -> {
                        showMessage("Home feature will be available soon")
                        true
                    }

                    binding.bottomNavigation.menu.getItem(1).itemId -> {
                        // Social item (current screen)
                        true
                    }
                    binding.bottomNavigation.menu.getItem(2).itemId -> {
                        // Settings item (for future)
                        showMessage("Settings will be available soon")
                        true
                    }
                    else -> false
                } 
            }
            
            // Set the Social item as selected
            selectedItemId = binding.bottomNavigation.menu.getItem(1).itemId
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.posts.collect { posts ->
                        adapter.submitList(posts)
                        binding.swipeRefresh.isRefreshing = false
                        binding.tvEmptyState.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading && !binding.swipeRefresh.isRefreshing) {
                            showLoading()
                        } else {
                            hideLoading()
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { errorMessage ->
                        errorMessage?.let {
                            showError(it)
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun showMessage(message: String) {
        showSuccessMessage(message)
    }

    override fun onResume() {
        super.onResume()
        // We only want to reload posts if we don't have any
        if (adapter.itemCount == 0) {
            viewModel.loadPosts()
        }
    }
} 