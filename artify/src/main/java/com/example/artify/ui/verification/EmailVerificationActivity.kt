package com.example.artify.ui.verification

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.databinding.ActivityEmailVerificationBinding
import com.example.artify.ui.login.LoginActivity
import com.example.artify.ui.profile.SetupUsernameActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private val viewModel: EmailVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
        
        // Kiểm tra trạng thái email verification ngay khi mở
        viewModel.checkEmailVerification()
    }

    override fun onResume() {
        super.onResume()
        // Kiểm tra lại khi user quay lại app (có thể đã xác thực email)
        viewModel.checkEmailVerification()
    }

    private fun setupViews() {
        binding.checkVerificationButton.setOnClickListener {
            viewModel.checkEmailVerification()
        }

        binding.resendEmailButton.setOnClickListener {
            viewModel.resendVerificationEmail()
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            navigateToLogin()
        }

        binding.testAuthButton.setOnClickListener {
            // Debug: kiểm tra auth state
            viewModel.debugAuthState()
        }
    }

    private fun observeViewModel() {
        viewModel.verificationState.observe(this) { state ->
            when (state) {
                is EmailVerificationState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.checkVerificationButton.isEnabled = false
                    binding.resendEmailButton.isEnabled = false
                }
                is EmailVerificationState.Verified -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    android.util.Log.d("EmailVerificationActivity", "Email đã được xác thực! Chuyển đến SetupUsernameActivity")
                    Toast.makeText(this, "Email đã được xác thực!", Toast.LENGTH_SHORT).show()
                    
                    // Chuyển đến setup username
                    val intent = Intent(this, SetupUsernameActivity::class.java)
                    android.util.Log.d("EmailVerificationActivity", "Bắt đầu SetupUsernameActivity")
                    startActivity(intent)
                    finish()
                }
                is EmailVerificationState.NotVerified -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.checkVerificationButton.isEnabled = true
                    binding.resendEmailButton.isEnabled = true
                    
                    binding.statusTextView.text = "Email chưa được xác thực. Vui lòng kiểm tra hộp thư của bạn."
                }
                is EmailVerificationState.EmailSent -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.checkVerificationButton.isEnabled = true
                    binding.resendEmailButton.isEnabled = true
                    
                    Toast.makeText(this, "Email xác thực đã được gửi lại!", Toast.LENGTH_SHORT).show()
                }
                is EmailVerificationState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.checkVerificationButton.isEnabled = true
                    binding.resendEmailButton.isEnabled = true
                    
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    
                    // Nếu phiên đăng nhập hết hạn, chuyển về login
                    if (state.message.contains("Phiên đăng nhập đã hết hạn") || 
                        state.message.contains("Không tìm thấy người dùng")) {
                        navigateToLogin()
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 