package com.example.artify.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.artify.R
import com.example.artify.databinding.ActivityRegisterBinding
import com.example.common.base.BaseActivity
import com.example.artify.ui.login.LoginActivity
import com.example.artify.ui.login.LoginState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {

    private val viewModel: RegisterViewModel by viewModels()
    override fun inflateBinding(): ActivityRegisterBinding {
        return ActivityRegisterBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.btnSignUp.setOnClickListener {
            val email = binding.edtInputEmail.text.toString().trim()
            val password = binding.edtInputPassword.text.toString().trim()
            val confirmPassword = binding.edtInputConfirmPassword.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                viewModel.registerWithEmail(email, password)
            }
        }

        binding.tvAlreadyAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.edtInputEmail.error = getString(R.string.error_email_empty)
            isValid = false
        } else {
            binding.edtInputEmail.error = null
        }

        if (password.isEmpty()) {
            binding.edtInputPassword.error = getString(R.string.error_password_empty)
            isValid = false
        } else if (password.length < 6) {
            binding.edtInputPassword.error = getString(R.string.error_password_length)
            isValid = false
        } else {
            binding.edtInputPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.edtInputConfirmPassword.error = getString(R.string.error_confirm_password_empty)
            isValid = false
        } else if (confirmPassword != password) {
            binding.edtInputConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        }
        return isValid
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    showLoading()
                }
                is LoginState.Success -> {
                    hideLoading()
                    // Gửi email xác thực
                    viewModel.sendEmailVerification()

                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_LONG).show()
                    
                    // Chuyển đến màn hình xác thực email
                    val intent = Intent(this, com.example.artify.ui.verification.EmailVerificationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is LoginState.Error -> {
                    hideLoading()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is LoginState.EmailVerificationSent -> {
                    Toast.makeText(this, getString(R.string.email_verification_sent), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    hideLoading()
                }
            }
        }
    }
} 