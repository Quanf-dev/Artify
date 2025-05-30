package com.example.artify.ui.editbase

import android.os.Bundle
// import android.view.ViewGroup // May not be needed if not dynamically adding to LinearLayout
// import android.widget.LinearLayout // May not be needed
import androidx.appcompat.app.AppCompatActivity
// import androidx.core.view.setPadding // Not directly used here
import androidx.viewbinding.ViewBinding
import com.example.artify.databinding.ActivityBaseEditBinding
// import com.mikepenz.iconics.IconicsDrawable // Not directly used here
// import com.mikepenz.iconics.typeface.IIcon // Not directly used here
// import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial // Not directly used here
import com.mikepenz.iconics.view.IconicsImageView
// import com.mikepenz.iconics.utils.colorRes // Not directly used here
// import com.mikepenz.iconics.utils.sizeDp // Not directly used here


abstract class BaseEditActivity<VB : ViewBinding> : AppCompatActivity() {

    // ViewBinding cho activity_base_edit.xml
    protected lateinit var baseBinding: ActivityBaseEditBinding

    // ViewBinding cho layout con (do lớp con cung cấp)
    protected lateinit var binding: VB

    // Iconics icons
    private lateinit var iconBack: IconicsImageView
    private lateinit var iconUndo: IconicsImageView
    private lateinit var iconRedo: IconicsImageView
    private lateinit var iconCheck: IconicsImageView

    // Lớp con phải override để cung cấp binding cho layout con
    abstract fun inflateBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate activity_base_edit.xml bằng binding
        baseBinding = ActivityBaseEditBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        // Inflate binding cho layout con, add vào container FrameLayout
        binding = inflateBinding()
        baseBinding.contentContainerEdit.addView(binding.root)

        setupCustomToolbar()
    }

    private fun setupCustomToolbar() {
        // We are not using setSupportActionBar anymore
        // supportActionBar?.setDisplayShowTitleEnabled(false)

        // Initialize icons from the custom_toolbar_overlay in ActivityBaseEditBinding
        iconBack = baseBinding.iconBack // Assuming ID is directly available on baseBinding
        iconUndo = baseBinding.iconUndo
        iconRedo = baseBinding.iconRedo
        iconCheck = baseBinding.iconCheck

        iconBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        iconUndo.setOnClickListener {
            onUndo()
        }
        iconRedo.setOnClickListener {
            onRedo()
        }
        iconCheck.setOnClickListener {
            onCheck()
        }
    }

    // Optional: Method to add more icons to 'toolbar_action_icons_group' if needed by subclasses
    /*
    protected fun addActionIconToToolbar(icon: IIcon, contentDescription: String, onClick: () -> Unit) {
        val iconView = IconicsImageView(this).apply {
            this.icon = IconicsDrawable(this@BaseEditActivity, icon).apply {
                colorRes = android.R.color.black // Or your desired color
                sizeDp = 24
            }
            this.contentDescription = contentDescription
            this.setOnClickListener { onClick() }
            this.setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless)
            this.setPadding(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)) // Example padding
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            this.layoutParams = lp
        }
        baseBinding.toolbarActionIconsGroup.addView(iconView) // Assuming toolbarActionIconsGroup is accessible
    }
    */

    protected open fun onUndo() {}
    protected open fun onRedo() {}
    protected open fun onCheck() {}
}
