package com.example.artify.ui.onboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.artify.model.Onboarding

class OnboardingAdapter(fragmentActivity: FragmentActivity, private val items: List<Onboarding>) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = mutableMapOf<Int, ScreenFragment>()

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        val fragment = ScreenFragment.newInstance(items[position])
        fragments[position] = fragment
        return fragment
    }

    fun getFragmentAt(position: Int): ScreenFragment? = fragments[position]
}

