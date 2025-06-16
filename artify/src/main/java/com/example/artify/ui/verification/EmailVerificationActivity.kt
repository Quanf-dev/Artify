package com.example.artify.ui.verification

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.artify.R
import com.example.artify.databinding.ActivityEmailVerificationBinding
import com.example.artify.databinding.DialogVerificationStatusBinding
import com.example.common.base.BaseActivity
import com.example.artify.ui.login.LoginActivity
import com.example.artify.ui.profile.SetupUsernameActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmailVerificationActivity : BaseActivity<ActivityEmailVerificationBinding>() {

    private val viewModel: EmailVerificationViewModel by viewModels()
    private var resendTimer: CountDownTimer? = null
    private var verificationDialog: Dialog? = null
    private var verificationRetryCount = 0

    override fun inflateBinding(): ActivityEmailVerificationBinding {
        return ActivityEmailVerificationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupListeners()
        observeViewModel()
        
        // Initial verification check
        lifecycleScope.launch {
            delay(500) // Small delay to ensure UI is ready
            viewModel.checkEmailVerification()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check verification status when returning to the activity
        viewModel.checkEmailVerification()
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        verificationDialog?.dismiss()
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.btnCheckVerification.setOnClickListener {
            verificationRetryCount++
            showLoading()
            // Force refresh the verification status from server
            viewModel.refreshAndCheckEmailVerification()
        }
        
        binding.btnResendEmail.setOnClickListener {
            viewModel.resendVerificationEmail()
        }
        
        binding.btnContinueAnyway?.setOnClickListener {
            showConfirmationDialog()
        }
        
        binding.tvLogout?.setOnClickListener {
            viewModel.logout()
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        viewModel.verificationState.observe(this) { state ->
            when (state) {
                is EmailVerificationState.Loading -> {
                    showLoading()
                    binding.btnResendEmail.isEnabled = false
                    binding.btnCheckVerification.isEnabled = false
                    binding.btnContinueAnyway?.visibility = View.GONE
                }
                
                is EmailVerificationState.Verified -> {
                    hideLoading()
                    // Only show success dialog when verification is successful
                    showStatusDialog(
                        isSuccess = true,
                        title = getString(R.string.verification_successful),
                        message = getString(R.string.email_verified_proceed_setup)
                    ) { navigateToSetupUsername() }
                }
                
                is EmailVerificationState.NotVerified -> {
                    hideLoading()
                    binding.btnResendEmail.isEnabled = true
                    binding.btnCheckVerification.isEnabled = true
                    binding.tvVerificationMessage.text =
                        getString(R.string.email_not_verified_check_mailbox)
                    
                    // Show "Continue Anyway" button after multiple verification attempts
                    if (verificationRetryCount >= 2) {
                        binding.btnContinueAnyway?.visibility = View.VISIBLE
                    } else {
                        binding.btnContinueAnyway?.visibility = View.GONE
                    }
                    
                    // Show toast to inform user verification is still pending
                    Toast.makeText(
                        this,
                        getString(R.string.email_not_verified_yet),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                is EmailVerificationState.EmailSent -> {
                    hideLoading()
                    binding.btnCheckVerification.isEnabled = true
                    Toast.makeText(
                        this,
                        getString(R.string.verification_email_resent_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    startResendEmailCountdown()
                }
                
                is EmailVerificationState.Error -> {
                    hideLoading()
                    binding.btnResendEmail.isEnabled = true
                    binding.btnCheckVerification.isEnabled = true

                    // Show "Continue Anyway" button after errors with verification
                    if (verificationRetryCount >= 2) {
                        binding.btnContinueAnyway?.visibility = View.VISIBLE
                    }

                    if (state.message.contains("Phiên đăng nhập đã hết hạn") ||
                        state.message.contains("Không tìm thấy người dùng")
                    ) {
                        showStatusDialog(
                            false,
                            getString(R.string.session_expired),
                            state.message
                        ) { navigateToLogin() }
                    } else if (state.message.contains("Không thể cập nhật trạng thái xác thực")) {
                        // Show specific message for verification status update failure
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                        
                        // Update UI to show the issue
                        binding.tvVerificationMessage.text = getString(R.string.verification_status_update_failed)
                    } else {
                        // Show error message
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun startResendEmailCountdown() {
        binding.btnResendEmail.isEnabled = false

        resendTimer = object : CountDownTimer(60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                binding.btnResendEmail.text =
                    getString(R.string.resend_in_seconds, secondsLeft)
            }

            override fun onFinish() {
                binding.btnResendEmail.isEnabled = true
                binding.btnResendEmail.text =
                    getString(R.string.resend_verification_email)
            }
        }.start()
    }

    private fun showStatusDialog(
        isSuccess: Boolean,
        title: String,
        message: String,
        onDismissAction: (() -> Unit)? = null
    ) {
        // Dismiss any existing dialog
        verificationDialog?.dismiss()
        
        val dialogBinding = DialogVerificationStatusBinding.inflate(LayoutInflater.from(this))
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)

        // Set dialog content
        dialogBinding.tvStatusTitle.text = title
        dialogBinding.tvStatusMessage.text = message
        
        // Set appropriate icon based on success/failure
        if (isSuccess) {
            dialogBinding.tvLabel.setImageResource(R.drawable.ic_success)
        } else {
            dialogBinding.tvLabel.setImageResource(R.drawable.ic_error)
        }
        
        // Add a button to dismiss the dialog
        dialogBinding.btnOk.setOnClickListener {
            dialog.dismiss()
            onDismissAction?.invoke()
        }
        
        // Store reference to dialog
        verificationDialog = dialog
        
        dialog.show()
    }
    
    private fun showConfirmationDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.continue_without_verification))
            .setMessage(getString(R.string.verification_continue_confirmation))
            .setPositiveButton(getString(R.string.continue_button)) { _, _ ->
                // User confirms they've verified their email
                navigateToSetupUsername()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        
        dialog.show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }

    private fun navigateToSetupUsername() {
        val intent = Intent(this, SetupUsernameActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}