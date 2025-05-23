package com.example.artify.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.artify.R
import com.example.artify.databinding.FragmentEmailLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailLoginFragment : Fragment() {

    private var _binding: FragmentEmailLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInput(email, password)) {
                viewModel.loginWithEmail(email, password)
            }
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInput(email, password)) {
                viewModel.registerWithEmail(email, password)
            }
        }

        binding.forgotPasswordText.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.resetPassword(email)
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
            }
        }

        binding.googleLoginButton.setOnClickListener {
            viewModel.loginWithGoogle(requireActivity())
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.googleLoginButton.isEnabled = false
                    binding.loginButton.isEnabled = false
                    binding.registerButton.isEnabled = false
                    Log.d("EmailLoginFragment", "Đang trong trạng thái Loading")
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.googleLoginButton.isEnabled = true
                    binding.loginButton.isEnabled = true
                    binding.registerButton.isEnabled = true
                    Log.d("EmailLoginFragment", "Đăng nhập thành công: ${state.user}")
                    Toast.makeText(requireContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    // Chuyển đến màn hình chính
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.googleLoginButton.isEnabled = true
                    binding.loginButton.isEnabled = true
                    binding.registerButton.isEnabled = true
                    Log.e("EmailLoginFragment", "Lỗi đăng nhập: ${state.message}")
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                is LoginState.PasswordResetEmailSent -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Email đặt lại mật khẩu đã được gửi", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email không được để trống"
            return false
        } else {
            binding.emailLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordLayout.error = "Mật khẩu không được để trống"
            return false
        } else if (password.length < 6) {
            binding.passwordLayout.error = "Mật khẩu phải có ít nhất 6 ký tự"
            return false
        } else {
            binding.passwordLayout.error = null
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}