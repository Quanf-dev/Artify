package com.example.artify.ui.frame

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.artify.R
import com.example.artify.databinding.ActivityFrameBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.ui.views.ImageFrameView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FrameActivity : BaseEditActivity<ActivityFrameBinding>() {

    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    override fun inflateBinding(): ActivityFrameBinding {
        return ActivityFrameBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root.findViewById<android.widget.LinearLayout>(R.id.tbFrame))

        // Initialize views

        toolbarBinding.ivDone.setOnClickListener {
            val editedBitmap = binding.imageFrameView.getCurrentBitmap()
            returnEditedImage(editedBitmap)
        }

        // Nhận ảnh đầu vào đồng bộ
        getInputBitmap(
            onBitmapReady = { bitmap ->
                binding.imageFrameView.setImageBitmap(bitmap)
                currentImageBitmap = bitmap
            },
            onError = {
                // Có thể load ảnh mẫu nếu muốn
            }
        )
    }

}