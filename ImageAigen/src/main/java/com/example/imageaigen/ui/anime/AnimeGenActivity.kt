package com.example.imageaigen.ui.anime

import android.os.Bundle
import androidx.activity.viewModels
import com.example.common.base.BaseActivity
import com.example.imageaigen.databinding.ActivityAnimeGenBinding
import com.example.imageaigen.utils.NavigationUtils

class AnimeGenActivity : BaseActivity<ActivityAnimeGenBinding>() {
    private val viewModel: AnimeGenViewModel by viewModels()

    override fun inflateBinding(): ActivityAnimeGenBinding {
        return ActivityAnimeGenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.generateButton.isEnabled = !isLoading
            if (isLoading) showLoading() else hideLoading()
        }

        viewModel.images.observe(this) { images ->
            if (images.isEmpty()) {
                showErrorMessage("No images generated")
                return@observe
            }
            NavigationUtils.navigateToPreview(this, images)
        }
    }

    private fun setupClickListeners() {
        binding.generateButton.setOnClickListener {
            val prompt = binding.promptEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                viewModel.generateAnimeImages(prompt)
            } else {
                showErrorMessage("Please enter a prompt")
            }
        }
    }
}