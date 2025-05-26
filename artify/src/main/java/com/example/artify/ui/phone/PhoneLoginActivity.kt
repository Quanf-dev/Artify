package com.example.artify.ui.phone

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.example.artify.databinding.ActivityPhoneLoginBinding
import com.example.artify.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneLoginActivity : BaseActivity<ActivityPhoneLoginBinding>() {

    private val viewModel: PhoneViewModel by viewModels()

    override fun inflateBinding(): ActivityPhoneLoginBinding {
        return ActivityPhoneLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.sendCodeButton.setOnClickListener {
            val phoneNumber = binding.phoneEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                viewModel.loginWithPhone(phoneNumber, this)
            } else {
                binding.phoneLayout.error = "Vui lòng nhập số điện thoại"
            }
        }

        binding.verifyCodeButton.setOnClickListener {
            val code = binding.codeEditText.text.toString().trim()
            if (code.isNotEmpty()) {
                viewModel.verifyPhoneNumberWithCode(code)
            } else {
                binding.codeLayout.error = "Vui lòng nhập mã xác thực"
            }
        }

    }

    private fun observeViewModel() {
        viewModel.phoneLoginState.observe(this) { state ->
            when (state) {
                is PhoneLoginState.Loading -> {
                    showLoading()
                }
                is PhoneLoginState.PhoneVerificationCodeSent -> {
                    hideLoading()
                    binding.codeLayout.visibility = View.VISIBLE
                    binding.verifyCodeButton.visibility = View.VISIBLE
                    Toast.makeText(this, "Mã xác thực đã được gửi", Toast.LENGTH_SHORT).show()
                }
                is PhoneLoginState.Success -> {
                    hideLoading()
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to the main screen or appropriate next screen
                    finish() // Finish for now
                }
                is PhoneLoginState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
                is PhoneLoginState.Idle -> {
                    hideLoading()
                }
            }
        }
    }
} 