package com.example.artify.ui.home

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.artify.ui.posts.PostsActivity
import com.example.artify.utils.EdgeToEdgeUtils
import com.example.artify.utils.navigate
import com.example.artify.utils.PermissionUtils
import com.example.imageaigen.ui.anime.AnimeGenActivity
import com.example.imageaigen.ui.cartoon.CartoonifyActivity
import com.example.imageaigen.ui.edit.EditImageActivity
import com.example.imageaigen.ui.generate.GenerateImageActivity
import com.example.imageaigen.ui.removebg.RemoveBackgroundActivity
import com.example.socialposts.ui.MainBottomNavigationHelper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.artify.R
import com.example.artify.databinding.ActivityHomeBinding
import com.example.artify.ui.editMain.EditMainActivity
import com.example.artify.ui.setting.SettingActivity
import com.example.artify.ui.camera.FaceFilterActivity
import com.example.common.base.BaseActivity
import com.example.imageaigen.ui.logo.LogoMakerActivity
import com.example.imageaigen.ui.toyfigure.ToyFigureActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, EditMainActivity::class.java)
            intent.putExtra("image_uri", it.toString())
            startActivity(intent)
        }
    }
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openCamera()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun inflateBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the app bar
        findViewById<View>(com.example.common.R.id.app_bar_layout_base).visibility = View.GONE
        findViewById<View>(com.example.common.R.id.coordinator_base).fitsSystemWindows = false

        setupClickListeners()
        setupFixedBottomNavigation()
        observeUserProfile()
    }
    
    private fun setupClickListeners() {
        // Set up click listener for select photo button
        binding.btnSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.frmAnimeGen.setOnClickListener{
            this.navigate(AnimeGenActivity::class.java)
        }
        binding.frmImageGen?.setOnClickListener{
            this.navigate(GenerateImageActivity::class.java)
        }
        binding.frmEditImage?.setOnClickListener{
            this.navigate(EditImageActivity::class.java)
        }
        binding.frmBgRemove.setOnClickListener{
            this.navigate(RemoveBackgroundActivity::class.java)
        }
        binding.frmCartoonGen.setOnClickListener{
            this.navigate(CartoonifyActivity::class.java)
        }
        binding.frmLogo?.setOnClickListener{
            this.navigate(LogoMakerActivity::class.java)
        }
        binding.frmActionFig?.setOnClickListener{
            this.navigate(ToyFigureActivity::class.java)
        }
        binding.frmCamera?.setOnClickListener{
            requestCameraPermissions()
        }
    }
    
    private fun observeUserProfile() {
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    showLoading()
                } else {
                    hideLoading()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.userProfile.collectLatest { profile ->
                profile?.let { updateUserInterface(it) }
            }
        }
        
        lifecycleScope.launch {
            viewModel.error.collectLatest { errorMessage ->
                errorMessage?.let { 
                    showErrorMessage(it)
                }
            }
        }
    }
    
    private fun updateUserInterface(profile: com.example.artify.model.UserProfile) {
        // Update username in the gradient text view
        binding.tvUsername?.text = profile.displayName
        
        // Log the photo URL for debugging
        android.util.Log.d("HomeActivity", "Avatar URL: ${profile.photoUrl}")
        
        // Load avatar image using Glide
        if (!profile.photoUrl.isNullOrEmpty()) {
            android.util.Log.d("HomeActivity", "Loading avatar from URL: ${profile.photoUrl}")
            Glide.with(this)
                .load(profile.photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.avatar_circle_1)
                .error(R.drawable.avatar_circle_1)
                .into(binding.imgAvatar)
        } else {
            android.util.Log.d("HomeActivity", "Loading default avatar")
            // Load default avatar if photoUrl is null or empty
            Glide.with(this)
                .load(R.drawable.avatar_circle_1)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.imgAvatar)
        }
    }

    private fun setupFixedBottomNavigation() {
        EdgeToEdgeUtils.setupCoordinatorLayoutWithBottomNav(
            activity = this,
            coordinatorLayout = binding.layout as CoordinatorLayout, // CoordinatorLayout root
            bottomNavigationView = binding.bottomNavigation
        )
        // Get the BottomNavigationView
        val bottomNav = binding.bottomNavigation
        
        // Apply animation
        MainBottomNavigationHelper.animateBottomNavigation(bottomNav)
        
        // Set up item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    // Already on home screen
                    true
                }
                R.id.menu_social -> {
                    navigate(PostsActivity::class.java)
                    true
                }
                R.id.menu_settings -> {
                    navigate(SettingActivity::class.java)
                    false
                }
                else -> false
            }
        }
    }
    
    private fun requestCameraPermissions() {
        if (PermissionUtils.hasCameraPermissions(this)) {
            openCamera()
        } else {
            PermissionUtils.requestCameraPermissions(cameraPermissionLauncher)
        }
    }
    
    private fun openCamera() {
        this.navigate(FaceFilterActivity::class.java)
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("Camera and storage permissions are required to use the camera feature. Please enable them in Settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(PermissionUtils.createAppSettingsIntent(this))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Camera permissions are required", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshUserProfile()
    }
}