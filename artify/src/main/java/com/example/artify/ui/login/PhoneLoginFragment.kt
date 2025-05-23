package com.example.artify.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.artify.databinding.FragmentPhoneLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneLoginFragment : Fragment() {

    private var _binding: FragmentPhoneLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by activityViewModels()
    private var verificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhoneLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.sendCodeButton.setOnClickListener {
            val phoneNumber = binding.phoneEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                viewModel.loginWithPhone(phoneNumber, requireActivity())
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
            }
        }

        binding.verifyCodeButton.setOnClickListener {
            val code = binding.codeEditText.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                viewModel.verifyPhoneNumberWithCode(verificationId!!, code)
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập mã xác thực", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is LoginState.PhoneVerificationSent -> {
                    binding.progressBar.visibility = View.GONE
                    binding.codeLayout.visibility = View.VISIBLE
                    binding.verifyCodeButton.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Mã xác thực đã được gửi", Toast.LENGTH_SHORT).show()
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    // Chuyển đến màn hình chính
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}