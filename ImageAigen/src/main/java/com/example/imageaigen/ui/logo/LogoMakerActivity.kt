package com.example.imageaigen.ui.logo

import android.os.Bundle
import androidx.activity.viewModels
import com.example.common.base.BaseActivity
import com.example.imageaigen.databinding.ActivityLogoMakerBinding
import com.example.imageaigen.utils.NavigationUtils

class LogoMakerActivity : BaseActivity<ActivityLogoMakerBinding>() {
    private val viewModel: LogoMakerViewModel by viewModels()

    override fun inflateBinding(): ActivityLogoMakerBinding {
        return ActivityLogoMakerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.generateLogoButton.isEnabled = !isLoading
            if (isLoading) showLoading() else hideLoading()
        }

        viewModel.images.observe(this) { images ->
            if (images.isEmpty()) {
                showErrorMessage("No logos generated")
                return@observe
            }
            NavigationUtils.navigateToPreview(this, images)
        }
    }

    private fun setupClickListeners() {
        binding.generateLogoButton.setOnClickListener {
            val name = binding.logoNameEditText.text.toString().trim()
            val description = binding.logoDescriptionEditText.text.toString().trim()
            
            if (name.isEmpty()) {
                showErrorMessage("Please enter a name for your logo")
                return@setOnClickListener
            }
            
            if (description.isEmpty()) {
                showErrorMessage("Please describe your logo")
                return@setOnClickListener
            }
            
            val prompt = "Create a professional logo for a company named '$name'. $description"
            viewModel.generateLogo(prompt)
        }
    }
} 