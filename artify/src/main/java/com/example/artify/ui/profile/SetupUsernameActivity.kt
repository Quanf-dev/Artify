package com.example.artify.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
// import android.graphics.drawable.Drawable // No longer needed here
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint
// import com.example.artify.ui.profile.AvatarAdapter // No longer directly used here

@AndroidEntryPoint
class SetupUsernameActivity : BaseActivity<ActivitySetupUsernameBinding>() {

    private val viewModel: SetupUsernameViewModel by viewModels()
    private var selectedAvatarUrl: String? = null
    private var currentAvatarList: List<String> = emptyList() // To store the fetched avatar list

    // The hardcoded list is removed from here
    // private val avatarUrls: List<String> = listOf(...)

    override fun inflateBinding(): ActivitySetupUsernameBinding {
        return ActivitySetupUsernameBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setupDefaultAvatar() is now called after avatar list is loaded
        setupViews()
        observeViewModel()
    }

    private fun setupDefaultAvatar(avatars: List<String>) {
        if (avatars.isEmpty()) return

        // Current user's photoUrl from ViewModel is prioritized
        val userPhotoUrl: String? = viewModel.currentUser.value?.photoUrl
        
        if (userPhotoUrl != null) {
            selectedAvatarUrl = userPhotoUrl
        } else if (selectedAvatarUrl == null) { // If no selected URL yet and no user photoUrl
            selectedAvatarUrl = avatars[0] // Default to the first avatar from the fetched list
        }
        // If selectedAvatarUrl is still null (e.g. empty list from server, though unlikely with current static list), use the first from list
        updateSelectedAvatarDisplay(selectedAvatarUrl ?: avatars.getOrNull(0))
    }

    private fun setupViews() {
        binding.btncOK?.setOnClickListener {
            val username: String = binding.usernameEditText.text.toString().trim()
            // Ensure a default is picked if somehow still null and list is not empty
            if (selectedAvatarUrl == null && currentAvatarList.isNotEmpty()) { 
                selectedAvatarUrl = currentAvatarList[0]
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
    }

    private fun showAvatarSelectionDialog(avatars: List<String>) {
        if (avatars.isEmpty()) {
            Toast.makeText(this, "No avatars available to select.", Toast.LENGTH_SHORT).show()
            return 
        }
        val dialog = AvatarSelectionDialogFragment.newInstance(avatars) { avatarUrl ->
            selectedAvatarUrl = avatarUrl
            updateSelectedAvatarDisplay(selectedAvatarUrl)
        }
        dialog.show(supportFragmentManager, AvatarSelectionDialogFragment.TAG)
    }

    private fun updateSelectedAvatarDisplay(avatarUrl: String?) {
        binding.selectedAvatarImageView.let { imageView ->
            avatarUrl?.let {
                if (imageView != null) {
                    Glide.with(this)
                        .load(it)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_foreground)
                        .circleCrop()
                        .into(imageView)
                }
            } ?: run {
                if (imageView != null) {
                    Glide.with(this)
                        .load(R.drawable.ic_launcher_background)
                        .circleCrop()
                        .into(imageView)
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
                    navigateToMain()
                }
                is SetupUsernameState.Error -> {
                    hideLoading()
                    showErrorMessage(state.message)
                    binding.usernameLayout.error = state.message
                }
                is SetupUsernameState.UsernameAlreadySet -> {
                    hideLoading()
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
            // selectedAvatarUrl will be primarily set by user interaction or default from availableAvatars list
            // If user object has photoUrl, it will be used to initialize selectedAvatarUrl via setupDefaultAvatar
            // We need to ensure setupDefaultAvatar is called after currentAvatarList is populated.
            if (currentAvatarList.isNotEmpty()) {
                 // If user photoUrl exists, it takes precedence. Otherwise, selectedAvatarUrl might be from previous selection or default.
                selectedAvatarUrl = user?.photoUrl ?: selectedAvatarUrl ?: currentAvatarList.getOrNull(0)
                updateSelectedAvatarDisplay(selectedAvatarUrl)
            }
        }

        viewModel.availableAvatars.observe(this) { avatars ->
            currentAvatarList = avatars
            // Now that we have the avatar list, setup the default avatar display
            // This also handles the initial avatar display based on currentUser data
            setupDefaultAvatar(avatars)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}