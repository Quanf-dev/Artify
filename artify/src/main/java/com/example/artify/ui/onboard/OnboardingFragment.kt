package com.example.artify.ui.onboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.artify.databinding.FragmentOnboardingBinding
import com.example.artify.model.Onboarding

class ScreenFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"
        private const val ARG_IMAGE = "image"

        fun newInstance(item: Onboarding): ScreenFragment {
            val fragment = ScreenFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, item.title)
            args.putString(ARG_DESC, item.description)
            args.putInt(ARG_IMAGE, item.imageResId)
            fragment.arguments = args
            return fragment
        }
    }

    fun playAnimation() {
        val duration = 500L
        val delayTitle = 100L
        val delayDesc = 300L
        val delayImage = 500L

        // Reset initial state
        binding.tvTitle.apply {
            alpha = 0f
            translationY = 50f
        }

        binding.tvDesc.apply {
            alpha = 0f
            translationY = 50f
        }

        binding.imageView.apply {
            alpha = 0f
            translationY = 50f
            scaleX = 0.8f
            scaleY = 0.8f
        }

        // Animate title
        binding.tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(delayTitle)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()

        // Animate description
        binding.tvDesc.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(delayDesc)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Animate image with scaling and bounce effect
        binding.imageView.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration + 200)
            .setStartDelay(delayImage)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        
        arguments?.let {
            binding.tvTitle.text = it.getString(ARG_TITLE)
            binding.tvDesc.text = it.getString(ARG_DESC)
            binding.imageView.setImageResource(it.getInt(ARG_IMAGE))
        }

        // Set initial state
        binding.tvTitle.alpha = 0f
        binding.tvTitle.translationY = 50f
        binding.tvDesc.alpha = 0f
        binding.tvDesc.translationY = 50f
        binding.imageView.alpha = 0f
        binding.imageView.translationY = 50f

        // Start initial animation
        playAnimation()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
