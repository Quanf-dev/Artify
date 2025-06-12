package com.example.artify

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.utils.ThemeHelper
import com.github.angads25.toggle.widget.DayNightSwitch

class TestzActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Áp dụng theme đã lưu
        ThemeHelper.applyTheme(ThemeHelper.getSavedTheme(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)



    }
}
