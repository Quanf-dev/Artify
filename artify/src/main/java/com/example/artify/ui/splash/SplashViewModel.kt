package com.example.artify.ui.splash

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "ArtifyPrefs"
        private const val KEY_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
    }

    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenOnboarding(): Boolean {
        return sharedPreferences.getBoolean(KEY_SEEN_ONBOARDING, false)
    }

    fun setOnboardingComplete() {
        sharedPreferences.edit().putBoolean(KEY_SEEN_ONBOARDING, true).apply()
    }

    fun setUserLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_USER_LOGGED_IN, isLoggedIn).apply()
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
    }
} 