package com.example.artify.ui.profile

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import com.example.artify.databinding.ActivitySetupUsernameBinding
import com.example.artify.ui.base.BaseActivity
import com.example.artify.utils.FullGradientDrawable
import com.example.artify.utils.dpToPx
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupUsernameActivity : BaseActivity<ActivitySetupUsernameBinding>() {

    private val viewModel: SetupUsernameViewModel by viewModels()

    override fun inflateBinding(): ActivitySetupUsernameBinding {
        return ActivitySetupUsernameBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.btnOK.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            viewModel.saveUsername(username)
        }
        val gradientBackground = FullGradientDrawable(
            cornerRadius = dpToPx(10).toFloat()
        )
        
        binding.btnOK.backgroundTintList = null
        binding.btnOK.background = gradientBackground
    }

    @SuppressLint("StringFormatMatches")
    private fun observeViewModel() {
        viewModel.setupState.observe(this) { state ->
            when (state) {
                is SetupUsernameState.Loading -> {
                    showLoading()
                }
                is SetupUsernameState.Success -> {
                    hideLoading()
                    navigateToMain()
                }
                is SetupUsernameState.Error -> {
                    hideLoading()
                    showErrorMessage(state.message)
                    binding.usernameLayout.error = state.message
                }
                is SetupUsernameState.UsernameAlreadySet -> {
                    hideLoading()
                    navigateToMain()
                }
                is SetupUsernameState.Idle -> {
                    hideLoading()
                    binding.btnOK.isEnabled = true
                }
            }
        }

        viewModel.currentUser.observe(this) { user ->
            // user?.username?.let { binding.usernameEditText.setText(it) }
        }
    }

    private fun navigateToMain() {
//        val intent = Intent(this, com.example.artify.ui.main.MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
    }
} 