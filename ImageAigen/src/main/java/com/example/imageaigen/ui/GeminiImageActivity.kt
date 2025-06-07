package com.example.imageaigen.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.imageaigen.R
import com.example.imageaigen.databinding.ActivityGeminiImageBinding
import com.example.imageaigen.ui.adapter.GeminiFragmentAdapter
import com.example.imageaigen.ui.fragments.EditImageFragment
import com.example.imageaigen.ui.viewmodel.GeminiViewModel
import com.google.android.material.tabs.TabLayoutMediator

class GeminiImageActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGeminiImageBinding
    private lateinit var viewModel: GeminiViewModel
    private lateinit var pagerAdapter: GeminiFragmentAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGeminiImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[GeminiViewModel::class.java]
        
        setupViewPager()
        setupToolbar()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupViewPager() {
        pagerAdapter = GeminiFragmentAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Generate"
                1 -> "Edit"
                else -> null
            }
        }.attach()
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> binding.toolbar.title = "Generate Image with AI"
                    1 -> binding.toolbar.title = "Edit Image with AI"
                }
            }
        })
    }
    
    fun toggleLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    fun navigateToEditWithImage(bitmap: Bitmap) {
        // Switch to Edit tab and pass the image
        binding.viewPager.currentItem = 1
        
        // Get reference to EditImageFragment and pass the bitmap
        val editFragment = pagerAdapter.getFragment(1) as? EditImageFragment
        editFragment?.setImageForEditing(bitmap)
    }
} 