package com.example.artify.ui.editbase

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.artify.databinding.ActivityBaseEditBinding
import com.example.artify.ui.crop.CropActivity
import com.example.artify.utils.navigate

abstract class BaseEditActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var baseBinding: ActivityBaseEditBinding
    protected lateinit var binding: VB

    abstract fun inflateBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseBinding = ActivityBaseEditBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = inflateBinding()
        baseBinding.contentContainerEdit.addView(binding.root)
    }

    protected fun navigateToPaint() {
        // TODO: Implement navigation to paint screen
    }

    protected fun navigateToText() {
        // TODO: Implement navigation to text screen
    }

    protected fun navigateToCrop() {
        navigate(CropActivity::class.java)
    }

    protected fun navigateToTune() {
        // TODO: Implement navigation to tune screen
    }

    protected fun navigateToFilter() {
        // TODO: Implement navigation to filter screen
    }

    protected fun navigateToBlur() {
        // TODO: Implement navigation to blur screen
    }

    protected fun navigateToEmoji() {
        // TODO: Implement navigation to emoji screen
    }

//    protected fun onBackPressed() {
//        finish()
//    }
}
