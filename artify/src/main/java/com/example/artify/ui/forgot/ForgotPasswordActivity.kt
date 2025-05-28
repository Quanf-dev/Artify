package com.example.artify.ui.forgot

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.artify.R
import com.example.artify.databinding.ActivityForgotPasswordBinding
import com.example.artify.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : BaseActivity<ActivityForgotPasswordBinding>() {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun inflateBinding(): ActivityForgotPasswordBinding {
        return ActivityForgotPasswordBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.forgotPasswordState.observe(this, Observer { state ->
            when (state) {
                is ForgotPasswordState.Idle -> {
                    hideLoading()
                }
                is ForgotPasswordState.Loading -> {
                    showLoading()
                }
                is ForgotPasswordState.EmailSentSuccess -> {
                    hideLoading()
                    showSuccessMessage(getString(R.string.reset_email_sent_success))
                    // Optionally finish activity after success
                    finish()
                }
                is ForgotPasswordState.Error -> {
                    hideLoading()
                    showErrorMessage(state.message)
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSendResetEmail?.setOnClickListener {
            val email = binding.edtEmailForgot.text.toString().trim()
            viewModel.sendPasswordResetEmail(email)
        }
    }

} 