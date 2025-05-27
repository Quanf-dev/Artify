package com.example.artify.utils

import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    const val LIGHT_MODE = "light"
    const val DARK_MODE = "dark"
    const val SYSTEM_MODE = "system"

    fun applyTheme(mode: String) {
        when (mode) {
            LIGHT_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            DARK_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            SYSTEM_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun saveTheme(context: android.content.Context, mode: String) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun getSavedTheme(context: android.content.Context): String {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("theme_mode", SYSTEM_MODE) ?: SYSTEM_MODE
    }
}
