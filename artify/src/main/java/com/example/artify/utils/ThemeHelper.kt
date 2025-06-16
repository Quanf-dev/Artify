package com.example.artify.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import com.example.artify.R
import kotlin.math.hypot
import androidx.core.view.ViewCompat

object ThemeHelper {

    const val LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO
    const val DARK_MODE = AppCompatDelegate.MODE_NIGHT_YES
    private const val ANIMATION_DURATION = 400L

    fun applyTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun saveTheme(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    fun getSavedTheme(context: Context): Int {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // Nếu đã từng lưu kiểu String, xóa nó đi
        if (prefs.contains("theme_mode") && prefs.all["theme_mode"] is String) {
            prefs.edit().remove("theme_mode").apply()
            return DARK_MODE
        }
        return prefs.getInt("theme_mode", DARK_MODE)
    }
    
    /**
     * Apply theme with a smooth transition animation
     * @param activity The activity to apply theme to
     * @param mode The theme mode to apply
     * @param switchView The view that triggered the theme change (for circular reveal)
     */
    fun applyThemeWithAnimation(activity: Activity, mode: Int, switchView: View? = null) {
        // Save theme preference first
        saveTheme(activity, mode)
        
        // If we're on Android 5.0+ and have a switch view, use circular reveal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && switchView != null) {
            // Get the center of the switch view for the reveal animation
            val cx = switchView.width / 2
            val cy = switchView.height / 2
            
            // Get the final radius for the circular reveal
            val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
            val finalRadius = hypot(rootView.width.toFloat(), rootView.height.toFloat())
            
            // Create the fade out animation
            val fadeOutAnim = AnimationUtils.loadAnimation(activity, R.anim.theme_fade_out)
            fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    // Apply theme when fade out completes
                    applyTheme(mode)
                    
                    // Wait a moment for the theme to apply
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Only animate if rootView is attached
                        if (ViewCompat.isAttachedToWindow(rootView)) {
                            // Only create reveal if switchView is attached
                            if (ViewCompat.isAttachedToWindow(switchView)) {
                                val anim = ViewAnimationUtils.createCircularReveal(
                                    rootView, cx, cy, 0f, finalRadius
                                )
                                anim.duration = ANIMATION_DURATION
                                // Make the root view visible and start reveal animation
                                rootView.visibility = View.VISIBLE
                                anim.start()
                            } else {
                                // Fallback: just make rootView visible
                                rootView.visibility = View.VISIBLE
                            }
                        }
                    }, 100)
                }
            })
            
            // Start fade out animation only if rootView is attached
            if (ViewCompat.isAttachedToWindow(rootView)) {
                rootView.startAnimation(fadeOutAnim)
            }
        } else {
            // Fallback to simple fade animation for older devices
            val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
            val fadeOutAnim = AnimationUtils.loadAnimation(activity, R.anim.theme_fade_out)
            
            fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    // Apply theme when fade out completes
                    applyTheme(mode)
                    
                    // Wait a moment for the theme to apply
                    Handler(Looper.getMainLooper()).postDelayed({
                        val fadeInAnim = AnimationUtils.loadAnimation(activity, R.anim.theme_fade_in)
                        if (ViewCompat.isAttachedToWindow(rootView)) {
                            rootView.startAnimation(fadeInAnim)
                        }
                    }, 100)
                }
            })
            
            // Start fade out animation only if rootView is attached
            if (ViewCompat.isAttachedToWindow(rootView)) {
                rootView.startAnimation(fadeOutAnim)
            }
        }
    }
}
