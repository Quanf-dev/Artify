package com.example.artify.ui.posts

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.artify.ui.home.HomeActivity
import com.example.artify.utils.navigate
import com.example.common.base.BaseActivity
import com.example.common.R
import com.example.socialposts.adapter.PostAdapter
import com.example.socialposts.databinding.ActivityPostsBinding
import com.example.socialposts.ui.CreatePostActivity
import com.example.socialposts.ui.MainBottomNavigationHelper
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
        // Hide the app bar
        findViewById<View>(R.id.app_bar_layout_base).visibility = View.GONE
        findViewById<View>(R.id.coordinator_base).fitsSystemWindows = false

        
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
                        navigate(HomeActivity::class.java)
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
                        binding.tvEmptyState.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            showLoading()
                        } else {
                            hideLoading()
                            binding.swipeRefresh.isRefreshing = false
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