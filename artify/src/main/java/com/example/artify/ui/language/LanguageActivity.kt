package com.example.artify.ui.language

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.artify.R
import com.example.artify.databinding.ActivityLanguageBinding
import com.example.artify.model.Language
import com.example.artify.utils.LocaleHelper
import com.example.common.base.BaseActivity
import com.zeugmasolutions.localehelper.Locales
import java.util.Locale

class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    private lateinit var adapter: LanguageAdapter
    private var languages = mutableListOf<Language>()

    override fun inflateBinding(): ActivityLanguageBinding = ActivityLanguageBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Toolbar is set up in BaseActivity, so just set title if needed
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Initialize RecyclerView
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        // Initialize languages
        setupLanguages()
        // Set up adapter
        adapter = LanguageAdapter(languages) { selectedLanguage ->
            changeLanguage(selectedLanguage)
        }
        binding.rvLanguages.adapter = adapter
    }

    private fun setupLanguages() {
        val currentLanguageCode = LocaleHelper.getSavedLanguage(this)
        languages.apply {
            add(Language("en", R.string.language_english, R.drawable.ic_flag_en, isSelected = currentLanguageCode == "en"))
            add(Language("vi", R.string.language_vietnamese, R.drawable.ic_flag_vi, isSelected = currentLanguageCode == "vi"))
            add(Language("es", R.string.language_spanish, R.drawable.ic_flag_es, isSelected = currentLanguageCode == "es"))
        }
    }
    private fun changeLanguage(language: Language) {
        // Lưu lại lựa chọn ngôn ngữ
        LocaleHelper.setLocale(this, language.code)
        // Cập nhật locale cho activity
        localeDelegate.setLocale(this, Locale(language.code))
        // Cập nhật trạng thái selected cho các ngôn ngữ
        languages.forEach { it.isSelected = it.code == language.code }
        adapter.notifyDataSetChanged()
        updateUI()
        Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        supportActionBar?.title = getString(R.string.language)
        binding.tvLanguageTitle.text = getString(R.string.language)
        binding.tvLanguageDescription.text = getString(R.string.language_description)
        adapter.notifyDataSetChanged()
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, languageCode))
    }
} 