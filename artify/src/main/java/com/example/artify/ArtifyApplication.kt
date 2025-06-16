package com.example.artify

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.example.artify.utils.ThemeHelper
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperApplicationDelegate
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class ArtifyApplication : Application() {
    private val localeAppDelegate = LocaleHelperApplicationDelegate()

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Apply saved theme (no animation needed on app startup)
        ThemeHelper.applyTheme(ThemeHelper.getSavedTheme(this))

        AppEventsLogger.activateApp(this)
    }


    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localeAppDelegate.attachBaseContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeAppDelegate.onConfigurationChanged(this)
    }

    override fun getApplicationContext(): Context =
        LocaleHelper.onAttach(super.getApplicationContext())
}