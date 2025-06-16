package com.example.artify.ui.phone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.artify.databinding.ActivityPhoneLoginBinding
import com.example.common.base.BaseActivity
import com.example.artify.ui.home.HomeActivity
import com.example.artify.ui.profile.SetupUsernameActivity
import com.example.artify.ui.splash.SplashViewModel
import com.example.artify.ui.verification.EmailVerificationActivity
import com.example.artify.utils.dpToPx
import com.example.common.gradiant4.GradientDotDrawable
import com.example.firebaseauth.FirebaseAuthManager
import com.example.firebaseauth.model.User
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhoneLoginActivity : BaseActivity<ActivityPhoneLoginBinding>() {

    private val viewModel: PhoneViewModel by viewModels()
    private val splashViewModel: SplashViewModel by viewModels()
    
    @Inject
    lateinit var authManager: FirebaseAuthManager
    
    // SharedPreferences constants
    private val PREFS_NAME = "ArtifyPrefs"
    private val KEY_USER_LOGGED_IN = "user_logged_in"
    private val KEY_USER_ID = "user_id"
    private val KEY_USERNAME = "username"
    private val KEY_EMAIL_VERIFIED = "email_verified"

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
                    
                    // Save user data to SharedPreferences
                    saveUserToPreferences(state.user)
                    
                    // Check if user has a username and avatar
                    checkUserProfileAndNavigate(state.user)
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
    
    private fun saveUserToPreferences(user: User) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean(KEY_USER_LOGGED_IN, true)
            putString(KEY_USER_ID, user.uid)
            putString(KEY_USERNAME, user.username)
            putBoolean(KEY_EMAIL_VERIFIED, user.isEmailVerified)
            apply()
        }
        
        // Also update the SplashViewModel
        splashViewModel.setUserLoggedIn(true)
        
        // Log detailed information
        Log.d("PhoneLoginActivity", "Saved user to preferences: uid=${user.uid}, username=${user.username}, email=${user.email}, isEmailVerified=${user.isEmailVerified}")
        
        // Verify the data was saved correctly
        val savedIsLoggedIn = sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
        val savedUserId = sharedPreferences.getString(KEY_USER_ID, null)
        val savedUsername = sharedPreferences.getString(KEY_USERNAME, null)
        Log.d("PhoneLoginActivity", "Verification - isLoggedIn: $savedIsLoggedIn, userId: $savedUserId, username: $savedUsername")
    }
    
    private fun checkUserProfileAndNavigate(user: User) {
        lifecycleScope.launch {
            try {
                // Get latest user data
                val currentUser = authManager.getCurrentUserWithUsername()
                
                if (currentUser != null) {
                    Log.d("PhoneLoginActivity", "Current user: ${currentUser.uid}, username: ${currentUser.username}")
                    
                    // Check if email verification is needed (usually not for phone login)
                    if (!currentUser.isEmailVerified && currentUser.email != null) {
                        startActivity(Intent(this@PhoneLoginActivity, EmailVerificationActivity::class.java))
                        finish()
                        return@launch
                    }
                    
                    // Check if username is set
                    if (currentUser.username.isNullOrEmpty()) {
                        Log.d("PhoneLoginActivity", "Username is empty, directing to profile setup")
                        startActivity(Intent(this@PhoneLoginActivity, SetupUsernameActivity::class.java))
                        finish()
                        return@launch
                    }
                    
                    // User is fully authenticated and profile is complete
                    Log.d("PhoneLoginActivity", "User is fully authenticated, going to HomeActivity")
                    startActivity(Intent(this@PhoneLoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    // Fallback to using the provided user object
                    if (user.username.isNullOrEmpty()) {
                        startActivity(Intent(this@PhoneLoginActivity, SetupUsernameActivity::class.java))
                    } else {
                        startActivity(Intent(this@PhoneLoginActivity, HomeActivity::class.java))
                    }
                    finish()
                }
            } catch (e: Exception) {
                Log.e("PhoneLoginActivity", "Error checking user profile: ${e.message}")
                // Fallback to simple navigation
                startActivity(Intent(this@PhoneLoginActivity, HomeActivity::class.java))
                finish()
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