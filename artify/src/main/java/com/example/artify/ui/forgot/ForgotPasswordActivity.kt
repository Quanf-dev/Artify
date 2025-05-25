package com.example.artify.ui.forgot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.databinding.ActivityForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
//    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
//        observeViewModel()
    }

    private fun setupViews() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.sendResetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (validateEmail(email)) {
//                viewModel.sendPasswordResetEmail(email)
            }
        }

        binding.backToLoginButton.setOnClickListener {
            finish()
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email không được để trống"
            return false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Email không hợp lệ"
            return false
        } else {
            binding.emailLayout.error = null
            return true
        }
    }

//    private fun observeViewModel() {
//        viewModel.resetState.observe(this) { state ->
//            when (state) {
//                is LoginState.Loading -> {
//                    binding.progressBar.visibility = android.view.View.VISIBLE
//                    binding.sendResetButton.isEnabled = false
//                }
//                is LoginState.PasswordResetEmailSent -> {
//                    binding.progressBar.visibility = android.view.View.GONE
//                    binding.sendResetButton.isEnabled = true
//
//                    // Hiển thị thông báo thành công
//                    binding.successLayout.visibility = android.view.View.VISIBLE
//                    binding.formLayout.visibility = android.view.View.GONE
//
//                    Toast.makeText(this, "Email đặt lại mật khẩu đã được gửi!", Toast.LENGTH_LONG).show()
//                }
//                is LoginState.Error -> {
//                    binding.progressBar.visibility = android.view.View.GONE
//                    binding.sendResetButton.isEnabled = true
//                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
//                }
//                else -> {
//                    binding.progressBar.visibility = android.view.View.GONE
//                    binding.sendResetButton.isEnabled = true
//                }
//            }
//        }
//    }
} 