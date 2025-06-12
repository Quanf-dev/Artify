package com.example.artify.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    private const val SELECTED_LANGUAGE = "selected_language"

    /**
     * Set the app's locale to the one specified by the given language code.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        saveLanguagePreference(context, languageCode)
        
        // Create or update the locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        // Update the app configuration
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        
        return context
    }

    /**
     * Get the current locale of the app.
     */
    fun getLocale(resources: Resources): Locale {
        val configuration = resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

    /**
     * Save the selected language code to shared preferences.
     */
    private fun saveLanguagePreference(context: Context, languageCode: String) {
        val preferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        preferences.edit().putString(SELECTED_LANGUAGE, languageCode).apply()
    }

    /**
     * Get the saved language code from shared preferences.
     */
    fun getSavedLanguage(context: Context): String {
        val preferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return preferences.getString(SELECTED_LANGUAGE, Locale.getDefault().language) ?: "en"
    }
} 