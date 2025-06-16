package com.example.common.base

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.airbnb.lottie.LottieAnimationView
import com.example.common.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.zeugmasolutions.localehelper.LocaleAwareCompatActivity
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegate
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegateImpl
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.util.Locale

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private lateinit var progressBar: LottieAnimationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var loadingScrim: BlurView
    protected lateinit var binding: VB
    private lateinit var contentContainer: FrameLayout
    val localeDelegate: LocaleHelperActivityDelegate = LocaleHelperActivityDelegateImpl()


    abstract fun inflateBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localeDelegate.onCreate(this)
        initializeLayout()
        setupToolbar()
    }

    private fun initializeLayout() {
        super.setContentView(R.layout.activity_base)
        
        contentContainer = findViewById(R.id.content_container_base)
        binding = inflateBinding()

        contentContainer.addView(binding.root)

        progressBar = findViewById(R.id.lottieProgressBar)
        toolbar = findViewById(R.id.toolbar_base)
        appBarLayout = findViewById(R.id.app_bar_layout_base)
        loadingScrim = findViewById(R.id.loading_scrim)

        val rootView = window.decorView.findViewById<ViewGroup>(R.id.content_container_base)
        val windowBackground: Drawable? = window.decorView.background

        loadingScrim.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(5f)

        loadingScrim.bringToFront()
        progressBar.bringToFront()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun setContentView(layoutResID: Int) {
        if (layoutResID != 0) {
            throw IllegalStateException("Use inflateBinding() instead of setContentView() in BaseActivity")
        }
    }

    protected fun showLoading() {
        loadingScrim.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    protected fun hideLoading() {
        loadingScrim.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    protected fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    protected fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    protected fun showSuccessMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun getDelegate() = localeDelegate.getAppCompatDelegate(super.getDelegate())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localeDelegate.attachBaseContext(newBase))
    }

    override fun onResume() {
        super.onResume()
        localeDelegate.onResumed(this)
    }

    override fun onPause() {
        super.onPause()
        localeDelegate.onPaused()
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        val context = super.createConfigurationContext(overrideConfiguration)
        return LocaleHelper.onAttach(context)
    }

    override fun getApplicationContext(): Context =
        localeDelegate.getApplicationContext(super.getApplicationContext())

    open fun updateLocale(locale: Locale) {
        localeDelegate.setLocale(this, locale)
    }

}
