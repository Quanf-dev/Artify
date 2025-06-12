package com.example.artify.ui.home

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.R
import com.example.artify.databinding.ActivityHomeBinding
import com.example.artify.ui.editMain.EditMainActivity
import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import com.example.artify.ui.setting.SettingActivity
import com.example.camera.filter.FaceFilterActivity
import com.example.common.base.BaseActivity

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

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
        binding.frmCamera?.setOnClickListener{
            requestCameraPermissions()
        }

        // Configure the bottom navigation as a fixed popup
        setupFixedBottomNavigation()
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
                    true
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
}