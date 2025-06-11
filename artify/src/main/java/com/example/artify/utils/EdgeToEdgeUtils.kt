package com.example.artify.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView

object EdgeToEdgeUtils {

    /**
     * Enable Edge-to-Edge display và xử lý system bars
     */
    fun enableEdgeToEdge(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        } else {
            // Android 10 và thấp hơn
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    /**
     * Setup BottomNavigationView để tránh che system navigation bar
     */
    fun setupBottomNavigationView(bottomNav: BottomNavigationView) {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarHeight = systemBarsInsets.bottom
            
            // Thêm padding bottom để tránh che navigation bar
            view.updatePadding(bottom = navigationBarHeight + view.paddingBottom)
            
            insets
        }
    }

    /**
     * Setup ScrollView hoặc content view để handle insets
     */
    fun setupContentView(contentView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarHeight = systemBarsInsets.top
            val navigationBarHeight = systemBarsInsets.bottom
            
            // Thêm padding để tránh che system bars
            view.updatePadding(
                top = statusBarHeight,
                bottom = navigationBarHeight
            )
            
            insets
        }
    }

    /**
     * Setup toàn bộ Edge-to-Edge cho Activity có BottomNavigationView
     */
    fun setupEdgeToEdgeWithBottomNav(
        activity: Activity,
        bottomNavigationView: BottomNavigationView,
        contentView: View? = null
    ) {
        // Enable Edge-to-Edge
        enableEdgeToEdge(activity)
        
        // Setup BottomNavigationView
        setupBottomNavigationView(bottomNavigationView)
        
        // Setup content view nếu có
        contentView?.let { setupContentView(it) }
        
        // Đảm bảo status bar có màu trong suốt
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        
        // Đảm bảo navigation bar có màu trong suốt
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    /**
     * Setup cho CoordinatorLayout với BottomNavigationView
     */
    fun setupCoordinatorLayoutWithBottomNav(
        activity: Activity,
        coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout,
        bottomNavigationView: BottomNavigationView
    ) {
        enableEdgeToEdge(activity)
        
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Chỉ apply top inset cho CoordinatorLayout
            view.updatePadding(top = systemBarsInsets.top)
            
            insets
        }
        
        // Setup BottomNavigationView riêng
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarHeight = systemBarsInsets.bottom
            
            // Update margin bottom thay vì padding
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = navigationBarHeight + 16 // 16dp margin gốc
            view.layoutParams = layoutParams
            
            insets
        }
        
        // Setup colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }
} 