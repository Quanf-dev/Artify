package com.example.artify.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.databinding.ActivityLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRegisterButton()
        setupPhoneLoginButton()
        setupFacebookLoginButton()


    }

    private fun setupRegisterButton() {
        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, com.example.artify.ui.register.RegisterActivity::class.java))
        }
    }

    private fun setupPhoneLoginButton() {
        binding.phoneLoginButton.setOnClickListener {
            startActivity(Intent(this, PhoneLoginActivity::class.java))
        }
    }

    private fun setupFacebookLoginButton() {
        binding.facebookLoginButton.setOnClickListener {
            // Sử dụng Facebook login thực
            viewModel.loginWithFacebook(this)
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        Log.d("LoginActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        if (requestCode == LoginViewModel.GOOGLE_SIGN_IN_REQUEST_CODE) {
            Log.d("LoginActivity", "Nhận kết quả đăng nhập Google")
            viewModel.handleGoogleSignInResult(data)
        } else {
            // Xử lý Facebook login result
            viewModel.handleFacebookActivityResult(requestCode, resultCode, data)
        }
    }
}