package com.example.artify.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.databinding.ActivitySetupUsernameBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupUsernameActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupUsernameBinding
    private val viewModel: SetupUsernameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("SetupUsernameActivity", "SetupUsernameActivity được tạo!")
        binding = ActivitySetupUsernameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.continueButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            if (validateUsername(username)) {
                viewModel.saveUsername(username)
            }
        }

    }

    private fun validateUsername(username: String): Boolean {
        if (username.isEmpty()) {
            binding.usernameLayout.error = "Tên người dùng không được để trống"
            return false
        } else if (username.length < 3) {
            binding.usernameLayout.error = "Tên người dùng phải có ít nhất 3 ký tự"
            return false
        } else if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            binding.usernameLayout.error = "Tên người dùng chỉ được chứa chữ cái, số và dấu gạch dưới"
            return false
        } else {
            binding.usernameLayout.error = null
            return true
        }
    }

    private fun observeViewModel() {
        viewModel.setupState.observe(this) { state ->
            when (state) {
                is SetupUsernameState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.continueButton.isEnabled = false
                }
                is SetupUsernameState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.continueButton.isEnabled = true
                    Toast.makeText(this, "Thiết lập tên người dùng thành công!", Toast.LENGTH_SHORT).show()
//                    navigateToMain()
                }
                is SetupUsernameState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.continueButton.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.continueButton.isEnabled = true
                }
            }
        }
    }

//    private fun navigateToMain() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
//    }
} 