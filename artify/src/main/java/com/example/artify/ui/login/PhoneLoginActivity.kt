package com.example.artify.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.databinding.ActivityPhoneLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
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
            if (code.isNotEmpty() && viewModel.verificationId != null) {
                viewModel.verifyPhoneNumberWithCode(viewModel.verificationId!!, code)
            } else {
                binding.codeLayout.error = "Vui lòng nhập mã xác thực"
            }
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                }
                is LoginState.PhoneVerificationSent -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.codeLayout.visibility = android.view.View.VISIBLE
                    binding.verifyCodeButton.visibility = android.view.View.VISIBLE
                    android.widget.Toast.makeText(this, "Mã xác thực đã được gửi", android.widget.Toast.LENGTH_SHORT).show()
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    android.widget.Toast.makeText(this, "Đăng nhập thành công", android.widget.Toast.LENGTH_SHORT).show()
                    finish()
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    android.widget.Toast.makeText(this, state.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }
} 