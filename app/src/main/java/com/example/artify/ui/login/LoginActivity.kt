package com.example.artify.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.artify.databinding.ActivityLoginBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = LoginPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Email"
                1 -> "Điện thoại"
                else -> null
            }
        }.attach()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        Log.d("LoginActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        if (requestCode == LoginViewModel.GOOGLE_SIGN_IN_REQUEST_CODE) {
            Log.d("LoginActivity", "Nhận kết quả đăng nhập Google")
            viewModel.handleGoogleSignInResult(data)
        }
    }
}

class LoginPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int) = when (position) {
        0 -> EmailLoginFragment()
        1 -> PhoneLoginFragment()
        else -> throw IllegalArgumentException("Invalid position $position")
    }
}