package com.example.artify.ui.editbase

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.artify.databinding.ActivityBaseEditBinding

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
}
