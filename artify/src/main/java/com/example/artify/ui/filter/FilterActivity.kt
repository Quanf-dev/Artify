package com.example.artify.ui.filter

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
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.artify.R
import com.example.artify.databinding.ActivityFilterBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.ui.views.ImageFilterView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilterActivity : BaseEditActivity<ActivityFilterBinding>() {

    private lateinit var imageFilterView: ImageFilterView
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    override fun inflateBinding(): ActivityFilterBinding {
        return ActivityFilterBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        imageFilterView = binding.imageFilterView
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root)

        // Nhận ảnh đầu vào đồng bộ
        getInputBitmap(
            onBitmapReady = { bitmap ->
                imageFilterView.setImageBitmap(bitmap)
                currentImageBitmap = bitmap
            },
            onError = {
                // Có thể load ảnh mẫu nếu muốn
            }
        )

        // Set up click listeners
        toolbarBinding.ivRedo.setOnClickListener { imageFilterView.resetFilters() }
        toolbarBinding.ivDone.setOnClickListener {
            // Get the edited image bitmap
            val editedBitmap = imageFilterView.getCurrentBitmap()
            // Return it to the EditMainActivity
            returnEditedImage(editedBitmap)
        }
    }



}