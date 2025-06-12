package com.example.artify.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.artify.R
import com.example.artify.utils.ThemeHelper

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        // Áp dụng theme trước khi super.onCreate
        val savedTheme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(savedTheme)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        radioGroup = findViewById(R.id.radioGroupTheme)

        // Set trạng thái radio button theo theme đã lưu
        when (savedTheme) {
            ThemeHelper.LIGHT_MODE -> radioGroup.check(R.id.radioLight)
            ThemeHelper.DARK_MODE -> radioGroup.check(R.id.radioDark)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.radioLight -> ThemeHelper.LIGHT_MODE
                R.id.radioDark -> ThemeHelper.DARK_MODE
                else -> ThemeHelper.DARK_MODE
            }
            ThemeHelper.saveTheme(this, selectedMode)
            ThemeHelper.applyTheme(selectedMode)

            // Reload activity để áp dụng theme mới
            recreate()
        }
    }
}
