package com.example.artify.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
import com.example.artify.R
import com.example.artify.databinding.ActivityLoginBinding
import com.example.common.base.BaseActivity
import com.example.artify.ui.phone.PhoneLoginActivity
import com.example.artify.ui.profile.SetupUsernameActivity
import com.example.artify.utils.dpToPx
import com.example.artify.utils.navigate
import com.example.common.gradiant4.GradientDotDrawable
import com.example.socialposts.ui.PostsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    override fun inflateBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupListeners()
        observeViewModel()
        setupUI()
    }

    private fun setupListeners() {
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, com.example.artify.ui.register.RegisterActivity::class.java))
        }

        binding.ivPhone.setOnClickListener {
            startActivity(Intent(this, PhoneLoginActivity::class.java))
        }

        binding.ivFacebook.setOnClickListener {
            viewModel.loginWithFacebook(this)
        }

        binding.ivGoogle.setOnClickListener {
            viewModel.loginWithGoogle(this)
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, com.example.artify.ui.forgot.ForgotPasswordActivity::class.java))
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.edtInputEmail.text.toString().trim()
            val password = binding.edtInputPassword.text.toString().trim()
            if (validateInput(email, password)) {
                viewModel.loginWithEmail(email, password)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    showLoading()
                    Log.d("LoginActivity", "Đang đăng nhập...")
                }
                is LoginState.Success -> {
                    hideLoading()
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    Log.d("LoginActivity", "Đăng nhập thành công: ${state.user}")
                    navigate(PostsActivity::class.java)
                }
                is LoginState.UsernameSetupRequired -> {
                    hideLoading()
                    Toast.makeText(this, getString(R.string.please_setup_username), Toast.LENGTH_SHORT).show()
                    navigate(SetupUsernameActivity::class.java)
                }
                is LoginState.Error -> {
                    hideLoading()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Lỗi đăng nhập: ${state.message}")
                }
                is LoginState.PasswordResetEmailSent -> {
                    hideLoading()
                    Toast.makeText(this, "Email đặt lại mật khẩu đã được gửi", Toast.LENGTH_SHORT).show()
                }
                is LoginState.EmailNotVerified -> {
                    hideLoading()
                    Toast.makeText(this, "Email chưa được xác thực. Chuyển đến màn hình xác thực.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, com.example.artify.ui.verification.EmailVerificationActivity::class.java))
                }
                else -> {
                    hideLoading()
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.edtInputEmail.error = "Email không được để trống"
            isValid = false
        } else {
            binding.edtInputEmail.error = null
        }

        if (password.isEmpty()) {
            binding.edtInputPassword.error = "Mật khẩu không được để trống"
            isValid = false
        } else if (password.length < 6) {
            binding.edtInputPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            isValid = false
        } else {
            binding.edtInputPassword.error = null
        }

        return isValid
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LoginViewModel.GOOGLE_SIGN_IN_REQUEST_CODE) {
            viewModel.handleGoogleSignInResult(data)
        } else {
            viewModel.handleFacebookActivityResult(requestCode, resultCode, data)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        val edtPassword = binding.edtInputPassword

        val gradientBorder = GradientDotDrawable    (
            height = dpToPx(2),
            cornerRadius = dpToPx(10).toFloat()
        )
        binding.edtInputEmail.background = gradientBorder
        binding.btnSignIn.backgroundTintList = null

        edtPassword.background = gradientBorder
        var isPasswordVisible = false

        edtPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableStart = 0 // drawableStart index
                if (event.rawX <= (edtPassword.left + edtPassword.compoundDrawables[drawableStart].bounds.width() + edtPassword.paddingStart)) {
                    if (isPasswordVisible) {
                        // Ẩn mật khẩu
                        edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        edtPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_eye, 0, 0, 0)
                        isPasswordVisible = false
                    } else {
                        // Hiển thị mật khẩu
                        edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        edtPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_eye_close, 0, 0, 0)
                        isPasswordVisible = true
                    }
                    edtPassword.setSelection(edtPassword.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

}
