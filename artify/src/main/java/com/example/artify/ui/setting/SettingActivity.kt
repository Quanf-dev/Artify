package com.example.artify.ui.setting

import android.content.Intent
import android.os.Bundle
import com.example.artify.ui.language.LanguageActivity
import com.example.artify.utils.ThemeHelper
import com.example.common.base.BaseActivity
import com.example.artify.databinding.ActivitySettingBinding

class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    override fun inflateBinding(): ActivitySettingBinding = ActivitySettingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Remove or move to BaseActivity if needed
        // setContentView(R.layout.activity_setting) // Remove manual setContentView

        val swDayNight = binding.swDayNight
        val lnLanguage = binding.linearLanguage

        // Đọc theme hiện tại và set lại trạng thái cho switch
        val currentTheme = ThemeHelper.getSavedTheme(this)
        swDayNight.isOn = (currentTheme == ThemeHelper.LIGHT_MODE)

        swDayNight.setOnToggledListener { _, isNight ->
            val selectedTheme = if (isNight) ThemeHelper.LIGHT_MODE else ThemeHelper.DARK_MODE
            
            // Use the new animation method instead of directly applying theme and recreating
            ThemeHelper.applyThemeWithAnimation(this@SettingActivity, selectedTheme, swDayNight)
            
            // No need to call recreate() as the animation handles the transition
        }
        lnLanguage.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
        }
    }
}