package com.example.artify.ui.phone

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import com.example.artify.databinding.ActivityPhoneLoginBinding
import com.example.artify.ui.base.BaseActivity
import com.example.artify.utils.GradientDotDrawable
import com.example.artify.utils.dpToPx
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
        setupUI()
    }

    private fun setupListeners() {
        binding.sendCodeButton.setOnClickListener {
            binding.edtPhone.apply {
                isFocusable = false
                isFocusableInTouchMode = false
                isCursorVisible = false
            }
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.edtPhone.windowToken, 0)
            if (binding.ccp.isValidFullNumber) {
                val fullPhoneNumber = binding.ccp.fullNumberWithPlus
                viewModel.loginWithPhone(fullPhoneNumber, this)
            } else {
                Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }

        binding.otpView.setOtpCompletionListener { otp ->
            binding.verifyCodeButton.isEnabled = true
        }

        binding.verifyCodeButton.setOnClickListener {
            val otp: String? = binding.otpView.text?.toString()
            if (!otp.isNullOrEmpty() && otp.length == 6) {
                viewModel.verifyPhoneNumberWithCode(otp)
            } else {
                Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show()
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
                    binding.otpView.visibility = View.VISIBLE
                    binding.verifyCodeButton.visibility = View.VISIBLE
                    binding.otpView.post {
                        binding.otpView.requestFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.otpView, InputMethodManager.SHOW_IMPLICIT)
                    }
                    Toast.makeText(this, "Mã xác thực đã được gửi", Toast.LENGTH_SHORT).show()
                }
                is PhoneLoginState.Success -> {
                    hideLoading()
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is PhoneLoginState.Error -> {
                    hideLoading()
                    showError(state.message)
                    binding.otpView.setText("")
                }
                is PhoneLoginState.Idle -> {
                    hideLoading()
                }
            }
        }
    }

    private fun setupUI() {
        binding.ccp.registerCarrierNumberEditText(binding.edtPhone)

        val gradientBorder = GradientDotDrawable(
            height = dpToPx(2),
            cornerRadius = dpToPx(10).toFloat()
        )
        binding.edtPhone.background = gradientBorder
        binding.verifyCodeButton.backgroundTintList = null
        binding.sendCodeButton.backgroundTintList = null
    }
} 