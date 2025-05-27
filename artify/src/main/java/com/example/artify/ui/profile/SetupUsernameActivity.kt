package com.example.artify.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
// import androidx.recyclerview.widget.LinearLayoutManager // No longer needed here
import com.bumptech.glide.Glide
import com.example.artify.R
import com.example.artify.databinding.ActivitySetupUsernameBinding
import com.example.artify.ui.base.BaseActivity
import com.example.artify.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
// import com.example.artify.ui.profile.AvatarAdapter // No longer directly used here

@AndroidEntryPoint
class SetupUsernameActivity : BaseActivity<ActivitySetupUsernameBinding>() {

    private val viewModel: SetupUsernameViewModel by viewModels()
    private var selectedAvatarUrl: String? = null

    private val avatarUrls: List<String> = listOf(
        "https://ia903406.us.archive.org/5/items/49_20210404/7.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/15.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/23.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/31.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/39.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/47.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/55.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/63.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/71.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/79.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/87.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/95.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/103.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/111.png",
        "https://ia903406.us.archive.org/5/items/49_20210404/119.png"
    )

    override fun inflateBinding(): ActivitySetupUsernameBinding {
        return ActivitySetupUsernameBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDefaultAvatar()
        setupViews()
        observeViewModel()
    }

    private fun setupDefaultAvatar() {
        // Set a default avatar if none is selected yet from the view model
        if (viewModel.currentUser.value?.photoUrl == null && avatarUrls.isNotEmpty()) {
            selectedAvatarUrl = avatarUrls[0]
        }
        updateSelectedAvatarDisplay(selectedAvatarUrl ?: avatarUrls.getOrNull(0))
    }

    private fun setupViews() {
        binding.btnOK.setOnClickListener {
            val username: String = binding.usernameEditText.text.toString().trim()
            if (selectedAvatarUrl == null && avatarUrls.isNotEmpty()) { // Ensure a default is picked if somehow still null
                selectedAvatarUrl = avatarUrls[0]
            }
            viewModel.saveUsernameAndAvatar(username, selectedAvatarUrl)
        }

        binding.selectedAvatarImageView?.setOnClickListener {
            showAvatarSelectionDialog()
        }
        binding.btnChooseAvatar?.setOnClickListener {
            showAvatarSelectionDialog()
        }
    }

    private fun showAvatarSelectionDialog() {
        val dialog = AvatarSelectionDialogFragment.newInstance(avatarUrls) { avatarUrl ->
            selectedAvatarUrl = avatarUrl
            updateSelectedAvatarDisplay(selectedAvatarUrl)
        }
        dialog.show(supportFragmentManager, AvatarSelectionDialogFragment.TAG)
    }

    private fun updateSelectedAvatarDisplay(avatarUrl: String?) {
        binding.selectedAvatarImageView?.let { imageView ->
            avatarUrl?.let {
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.ic_launcher_background) // Default placeholder
                    .error(R.drawable.ic_launcher_foreground) // Error placeholder
                    .circleCrop()
                    .into(imageView)
            } ?: run {
                // Set a default image if URL is null
                Glide.with(this)
                    .load(R.drawable.ic_launcher_background) // Your default avatar drawable
                    .circleCrop()
                    .into(imageView)
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
                    binding.btnOK.isEnabled = true
                }
            }
        }

        viewModel.currentUser.observe(this) { user ->
            user?.username?.let { binding.usernameEditText.setText(it) }
            // Update selected avatar based on user data, or default if not set
            selectedAvatarUrl = user?.photoUrl ?: avatarUrls.getOrNull(0)
            updateSelectedAvatarDisplay(selectedAvatarUrl)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}