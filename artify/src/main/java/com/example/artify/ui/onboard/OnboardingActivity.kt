package com.example.artify.ui.onboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.artify.R
import com.example.artify.databinding.ActivityOnboardingBinding
import com.example.artify.ui.login.LoginActivity
import com.example.artify.ui.splash.SplashViewModel
import com.example.common.gradiant4.GradientDotDrawable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val onboardingItems = getOnboardingItems(this)
        adapter = OnboardingAdapter(this, onboardingItems)
        binding.viewPager?.adapter = adapter

        binding.viewPager?.setPageTransformer { page, position ->
            val absPos = kotlin.math.abs(position)
            page.alpha = 1 - absPos.coerceIn(0f, 1f)
            page.scaleX = 1 - 0.25f * absPos
            page.scaleY = 1 - 0.25f * absPos
        }
        binding.viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val fragment = adapter.getFragmentAt(position)
                fragment?.playAnimation()
                
                // Update button text on last page
                if (position == adapter.itemCount - 1) {
                    binding.btnNext.text = getString(R.string.get_started)
                } else {
                    binding.btnNext.text = getString(R.string.next)
                }
            }
        })


        // Tạo dot drawable
        val activeDot = GradientDotDrawable(
            width = dpToPx(32),
            height = dpToPx(6),
            cornerRadius = dpToPx(10).toFloat()
        )
        val inactiveDot = ContextCompat.getDrawable(this, R.drawable.inactive_dot)!!

        // Gán drawable
        binding.customDotsIndicator?.setSelectedDotDrawable(activeDot)
        binding.customDotsIndicator?.setUnselectedDotDrawable(inactiveDot)


        // Kết nối viewpager
        binding.customDotsIndicator?.setupWithViewPager(binding.viewPager!!, adapter.itemCount)

        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager?.currentItem ?: 0
            if (currentItem < adapter.itemCount - 1) {
                binding.viewPager?.currentItem = currentItem + 1
            } else {
                // Last page - complete onboarding
                completeOnboarding()
            }
        }
        
        binding.tvSkip.setOnClickListener {
            completeOnboarding()
        }
        
        binding.viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val fragment = adapter.getFragmentAt(position)
                fragment?.playAnimation()
                binding.tvSkip.visibility =
                    if (position == adapter.itemCount - 1) View.GONE else View.VISIBLE
            }
        })
    }
    
    private fun completeOnboarding() {
        // Mark onboarding as completed
        viewModel.setOnboardingComplete()
        
        // Navigate to login screen
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

