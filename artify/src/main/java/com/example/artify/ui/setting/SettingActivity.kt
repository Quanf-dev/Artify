package com.example.artify.ui.setting

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.artify.ui.language.LanguageActivity
import com.example.artify.ui.login.LoginActivity
import com.example.artify.utils.ThemeHelper
import com.example.common.base.BaseActivity
import com.example.artify.databinding.ActivitySettingBinding
import com.example.firebaseauth.FirebaseAuthManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.artify.R
import com.example.artify.model.UserProfile
import android.content.Context

@AndroidEntryPoint
class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    
    @Inject
    lateinit var authManager: FirebaseAuthManager
    
    override fun inflateBinding(): ActivitySettingBinding = ActivitySettingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Remove or move to BaseActivity if needed
        // setContentView(R.layout.activity_setting) // Remove manual setContentView

        val swDayNight = binding.swDayNight
        val lnLanguage = binding.linearLanguage

        // Đọc theme hiện tại và set lại trạng thái cho switch
        val currentTheme = ThemeHelper.getSavedTheme(this)
        swDayNight.isOn = (currentTheme == ThemeHelper.LIGHT_MODE)

        swDayNight.setOnToggledListener { _, isNight ->
            val selectedTheme = if (isNight) ThemeHelper.LIGHT_MODE else ThemeHelper.DARK_MODE
            
            // Use the new animation method instead of directly applying theme and recreating
            ThemeHelper.applyThemeWithAnimation(this@SettingActivity, selectedTheme, swDayNight)
            
            // No need to call recreate() as the animation handles the transition
        }
        
        lnLanguage.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
        }
        
        binding.lnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        
        // Load user profile data
        loadUserProfileData()
    }
    
    private fun loadUserProfileData() {
        lifecycleScope.launch {
            showLoading()
            try {
                val user = authManager.getCurrentUserWithUsername()
                user?.let {
                    val userProfile = UserProfile(
                        uid = it.uid,
                        displayName = it.username ?: "User",
                        email = it.email,
                        phoneNumber = it.phoneNumber,
                        photoUrl = it.photoUrl,
                        isEmailVerified = it.isEmailVerified,
                        providerId = "firebase" // Since we don't have providerData in our User model
                    )
                    updateUI(userProfile)
                }
            } catch (e: Exception) {
                showErrorMessage(e.message ?: "Failed to load user profile")
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun updateUI(profile: UserProfile) {
        // Update username
        binding.tvUserName.text = profile.displayName
        
        // Update email
        binding.tvUserMail.text = profile.email ?: "No email"
        
        // Log the photo URL for debugging
        android.util.Log.d("SettingActivity", "Avatar URL: ${profile.photoUrl}")
        
        // Load avatar image using Glide
        if (!profile.photoUrl.isNullOrEmpty()) {
            android.util.Log.d("SettingActivity", "Loading avatar from URL: ${profile.photoUrl}")
            Glide.with(this)
                .load(profile.photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.avatar_circle_1)
                .error(R.drawable.avatar_circle_1)
                .into(binding.imgAvatar)
        } else {
            android.util.Log.d("SettingActivity", "Loading default avatar")
            // Load default avatar if photoUrl is null or empty
            Glide.with(this)
                .load(R.drawable.avatar_circle_1)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.imgAvatar)
        }
    }
    
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch { // Use lifecycleScope.launch for coroutines
                    performLogout()
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private suspend fun performLogout() {
        // Show loading indicator
        showLoading()
        
        // Perform logout operation
        authManager.signOut()
        
        // Clear SharedPreferences
        val sharedPreferences = getSharedPreferences("ArtifyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("user_logged_in", false)
            remove("user_id")
            remove("username")
            remove("email_verified")
            apply()
        }
        
        // Hide loading indicator
        hideLoading()
        
        // Show success message
        showSuccessMessage("Successfully logged out")
        
        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to this screen
        loadUserProfileData()
    }
}