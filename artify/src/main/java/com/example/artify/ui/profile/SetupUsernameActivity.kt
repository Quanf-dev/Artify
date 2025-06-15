package com.example.artify.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
// import android.graphics.drawable.Drawable // No longer needed here
import android.os.Bundle
import android.util.Log
import android.view.View
// import android.view.View // No longer needed here
import android.widget.Toast // Added import for Toast
import androidx.activity.viewModels
// import androidx.recyclerview.widget.LinearLayoutManager // No longer needed here
import com.bumptech.glide.Glide
// import com.bumptech.glide.load.DataSource // No longer needed here
// import com.bumptech.glide.load.engine.GlideException // No longer needed here
// import com.bumptech.glide.request.RequestListener // No longer needed here
// import com.bumptech.glide.request.target.Target // No longer needed here
import com.example.artify.R
import com.example.artify.databinding.ActivitySetupUsernameBinding
import com.example.common.base.BaseActivity
import com.example.artify.ui.home.HomeActivity
import com.example.artify.ui.splash.SplashViewModel
import com.example.firebaseauth.model.User
import dagger.hilt.android.AndroidEntryPoint
// import com.example.artify.ui.profile.AvatarAdapter // No longer directly used here

@AndroidEntryPoint
class SetupUsernameActivity : BaseActivity<ActivitySetupUsernameBinding>() {

    private val viewModel: SetupUsernameViewModel by viewModels()
    private val splashViewModel: SplashViewModel by viewModels()
    private var selectedAvatarUrl: String? = null
    private var currentAvatarList: List<String> = emptyList() // To store the fetched avatar list

    // Đường dẫn mặc định cho avatar
    private val defaultAvatarUrl = "default_avatar.png"
    
    // SharedPreferences constants
    private val PREFS_NAME = "ArtifyPrefs"
    private val KEY_USER_LOGGED_IN = "user_logged_in"
    private val KEY_USER_ID = "user_id"
    private val KEY_USERNAME = "username"
    private val KEY_EMAIL_VERIFIED = "email_verified"

    // The hardcoded list is removed from here
    // private val avatarUrls: List<String> = listOf(...)

    override fun inflateBinding(): ActivitySetupUsernameBinding {
        return ActivitySetupUsernameBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupDefaultAvatar(avatars: List<String>) {
        if (avatars.isEmpty()) return

        // Kiểm tra nếu người dùng đã có avatar từ trước
        val userPhotoUrl: String? = viewModel.currentUser.value?.photoUrl
        
        if (userPhotoUrl != null && userPhotoUrl != defaultAvatarUrl && avatars.contains(userPhotoUrl)) {
            // Nếu người dùng đã có avatar hợp lệ
            selectedAvatarUrl = userPhotoUrl
            updateSelectedAvatarDisplay(selectedAvatarUrl)
        } else {
            // Hiển thị giao diện chọn avatar
            selectedAvatarUrl = null
            updateSelectedAvatarDisplay(null)
            // Hiển thị thông báo yêu cầu chọn avatar
            binding.avatarSelectionHint?.visibility = View.VISIBLE
        }
    }

    private fun setupViews() {
        binding.btncOK?.setOnClickListener {
            val username: String = binding.usernameEditText.text.toString().trim()
            
            // Kiểm tra xem người dùng đã chọn avatar chưa
            if (selectedAvatarUrl == null || selectedAvatarUrl == defaultAvatarUrl) {
                Toast.makeText(this, getString(R.string.avatar_not_selected_error), Toast.LENGTH_SHORT).show()
                // Hiển thị giao diện chọn avatar
                if (currentAvatarList.isNotEmpty()) {
                    showAvatarSelectionDialog(currentAvatarList)
                }
                return@setOnClickListener
            }
            
            viewModel.saveUsernameAndAvatar(username, selectedAvatarUrl)
        }

        // Ensure currentAvatarList is not empty before showing dialog
        val avatarClickListener = { _: android.view.View ->
            if (currentAvatarList.isNotEmpty()) {
                showAvatarSelectionDialog(currentAvatarList)
            } else {
                Toast.makeText(this, "Avatars are loading, please wait...", Toast.LENGTH_SHORT).show()
            }
        }
        binding.selectedAvatarImageView?.setOnClickListener(avatarClickListener)
        binding.tvChooseAvatar?.setOnClickListener(avatarClickListener)
        
        // Hiển thị thông báo yêu cầu chọn avatar
        binding.avatarSelectionHint?.visibility = View.VISIBLE
    }

    private fun showAvatarSelectionDialog(avatars: List<String>) {
        if (avatars.isEmpty()) {
            Toast.makeText(this, "No avatars available to select.", Toast.LENGTH_SHORT).show()
            return 
        }
        val dialog = AvatarSelectionDialogFragment.newInstance(avatars) { avatarUrl ->
            selectedAvatarUrl = avatarUrl
            updateSelectedAvatarDisplay(selectedAvatarUrl)
            // Ẩn thông báo yêu cầu chọn avatar
            binding.avatarSelectionHint?.visibility = View.GONE
        }
        dialog.show(supportFragmentManager, AvatarSelectionDialogFragment.TAG)
    }

    private fun updateSelectedAvatarDisplay(avatarUrl: String?) {
        binding.selectedAvatarImageView.let { imageView ->
            if (imageView != null) {
                if (avatarUrl == null || avatarUrl == defaultAvatarUrl) {
                    // Hiển thị placeholder cho avatar
                    Glide.with(this)
                        .load(R.drawable.default_avatar)
                        .circleCrop()
                        .into(imageView)
                    
                    // Hiển thị thông báo yêu cầu chọn avatar
                    binding.avatarSelectionHint?.visibility = View.VISIBLE
                } else {
                    // Hiển thị avatar đã chọn
                    Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .circleCrop()
                        .into(imageView)
                    
                    // Ẩn thông báo yêu cầu chọn avatar
                    binding.avatarSelectionHint?.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun observeViewModel() {
        viewModel.setupState.observe(this) { state ->
            when (state) {
                is SetupUsernameState.Loading -> showLoading()
                is SetupUsernameState.Success -> {
                    hideLoading()
                    // Save user login state
                    splashViewModel.setUserLoggedIn(true)
                    
                    // Save user data to SharedPreferences
                    viewModel.currentUser.value?.let { saveUserToPreferences(it) }
                    
                    navigateToMain()
                }
                is SetupUsernameState.Error -> {
                    hideLoading()
                    showErrorMessage(state.message)
                    binding.usernameLayout.error = state.message
                }
                is SetupUsernameState.UsernameAlreadySet -> {
                    hideLoading()
                    // Save user login state
                    splashViewModel.setUserLoggedIn(true)
                    
                    // Save user data to SharedPreferences
                    viewModel.currentUser.value?.let { saveUserToPreferences(it) }
                    
                    navigateToMain()
                }
                is SetupUsernameState.Idle -> {
                    hideLoading()
                    binding.btncOK?.isEnabled = true
                }
            }
        }

        viewModel.currentUser.observe(this) { user ->
            user?.username?.let { binding.usernameEditText.setText(it) }
            
            // Kiểm tra nếu người dùng đã có avatar từ trước
            val userPhotoUrl = user?.photoUrl
            if (userPhotoUrl != null && userPhotoUrl != defaultAvatarUrl && currentAvatarList.contains(userPhotoUrl)) {
                selectedAvatarUrl = userPhotoUrl
                updateSelectedAvatarDisplay(selectedAvatarUrl)
            }
        }

        viewModel.availableAvatars.observe(this) { avatars ->
            currentAvatarList = avatars
            setupDefaultAvatar(avatars)
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
        Log.d("SetupUsernameActivity", "Saved user to preferences: uid=${user.uid}, username=${user.username}, email=${user.email}, isEmailVerified=${user.isEmailVerified}")
        
        // Verify the data was saved correctly
        val savedIsLoggedIn = sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
        val savedUserId = sharedPreferences.getString(KEY_USER_ID, null)
        val savedUsername = sharedPreferences.getString(KEY_USERNAME, null)
        Log.d("SetupUsernameActivity", "Verification - isLoggedIn: $savedIsLoggedIn, userId: $savedUserId, username: $savedUsername")
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}