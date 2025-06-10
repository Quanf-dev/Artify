package com.example.socialposts.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationView

object MainBottomNavigationHelper {
    
    fun animateBottomNavigation(bottomNav: BottomNavigationView) {
        // Initial setup - hide the bottom nav
        bottomNav.translationY = 100f
        bottomNav.alpha = 0f
        
        // Animate the bottom nav to appear with a slight bounce
        bottomNav.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    fun setupBottomNavItemAnimation(bottomNav: BottomNavigationView) {
        // Add animation to item selection
        bottomNav.setOnItemSelectedListener { item ->
            // Find the view for the selected item
            val itemView = bottomNav.findViewById<View>(item.itemId)
            
            // Create a bounce animation
            val animator = ValueAnimator.ofFloat(1f, 1.2f, 1f)
            animator.duration = 300
            animator.addUpdateListener { animation ->
                itemView?.scaleX = animation.animatedValue as Float
                itemView?.scaleY = animation.animatedValue as Float
            }
            animator.start()
            
            true
        }
    }
} 