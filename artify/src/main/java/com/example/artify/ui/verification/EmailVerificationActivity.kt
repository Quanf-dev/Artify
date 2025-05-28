package com.example.artify.ui.verification

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.Window
import android.widget.Toast
import androidx.activity.viewModels
import com.example.artify.R
import com.example.artify.databinding.ActivityEmailVerificationBinding
import com.example.artify.databinding.DialogVerificationStatusBinding
import com.example.artify.ui.base.BaseActivity
import com.example.artify.ui.login.LoginActivity
import com.example.artify.ui.profile.SetupUsernameActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class EmailVerificationActivity : BaseActivity<ActivityEmailVerificationBinding>() {

    private val viewModel: EmailVerificationViewModel by viewModels()
    private var resendTimer: CountDownTimer? = null

    override fun inflateBinding(): ActivityEmailVerificationBinding {
        return ActivityEmailVerificationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupListeners()
        observeViewModel()
        viewModel.checkEmailVerification()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkEmailVerification()
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.btnCheckVerification.setOnClickListener {
            viewModel.checkEmailVerification()
        }
        binding.btnResendEmail.setOnClickListener {
            viewModel.resendVerificationEmail()
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
                }
                is EmailVerificationState.Verified -> {
                    hideLoading()
                    showStatusDialog(
                        isSuccess = true,
                        title = getString(R.string.verification_successful),
                        message = getString(R.string.email_verified_proceed_setup)
                    ) { navigateToSetupUsername() }
                }
                is EmailVerificationState.NotVerified -> {
                    hideLoading()
                    binding.btnResendEmail.isEnabled = true
                    binding.tvVerificationMessage.text =
                        getString(R.string.email_not_verified_check_mailbox)
                }
                is EmailVerificationState.EmailSent -> {
                    hideLoading()
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

                    if (state.message.contains("Phiên đăng nhập đã hết hạn") ||
                        state.message.contains("Không tìm thấy người dùng")
                    ) {
                        showStatusDialog(
                            false,
                            getString(R.string.session_expired),
                            state.message
                        ) { navigateToLogin() }
                    } else {
                        showStatusDialog(false, getString(R.string.error_occurred), state.message)
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
        val dialogBinding =
            DialogVerificationStatusBinding.inflate(LayoutInflater.from(this))
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)

        dialogBinding.tvStatusTitle.text = title
        dialogBinding.tvStatusMessage.text = message

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