package com.example.common.base

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.common.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var loadingScrim: BlurView
    protected lateinit var binding: VB
    private lateinit var contentContainer: FrameLayout

    abstract fun inflateBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeLayout()
        setupToolbar()
    }

    private fun initializeLayout() {
        super.setContentView(R.layout.activity_base)
        
        contentContainer = findViewById(R.id.content_container_base)
        binding = inflateBinding()

        contentContainer.addView(binding.root)

        progressBar = findViewById(R.id.globalProgressBar)
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
}
