package com.example.artify.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.databinding.ActivityRegisterBinding
import com.example.artify.ui.login.LoginActivity
import com.example.artify.ui.login.LoginState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                viewModel.registerWithEmail(email, password)
            }
        }

        binding.loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email không được để trống"
            return false
        } else {
            binding.emailLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordLayout.error = "Mật khẩu không được để trống"
            return false
        } else if (password.length < 6) {
            binding.passwordLayout.error = "Mật khẩu phải có ít nhất 6 ký tự"
            return false
        } else {
            binding.passwordLayout.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordLayout.error = "Xác nhận mật khẩu không được để trống"
            return false
        } else if (password != confirmPassword) {
            binding.confirmPasswordLayout.error = "Mật khẩu không khớp"
            return false
        } else {
            binding.confirmPasswordLayout.error = null
        }

        return true
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.registerButton.isEnabled = false
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.registerButton.isEnabled = true
                    
                    // Gửi email xác thực
                    viewModel.sendEmailVerification()
                    
                    Toast.makeText(this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", Toast.LENGTH_LONG).show()
                    
                    // Chuyển đến màn hình xác thực email
                    val intent = Intent(this, com.example.artify.ui.verification.EmailVerificationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.registerButton.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is LoginState.EmailVerificationSent -> {
                    Toast.makeText(this, "Email xác thực đã được gửi!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.registerButton.isEnabled = true
                }
            }
        }
    }
} 