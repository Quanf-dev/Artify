package com.example.imageaigen.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.imageaigen.ui.fragments.EditImageFragment
import com.example.imageaigen.ui.fragments.GenerateImageFragment

class GeminiFragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    private val fragments = mutableMapOf<Int, Fragment>()
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> GenerateImageFragment()
            1 -> EditImageFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
        
        fragments[position] = fragment
        return fragment
    }
    
    fun getFragment(position: Int): Fragment? {
        return fragments[position]
    }
} 