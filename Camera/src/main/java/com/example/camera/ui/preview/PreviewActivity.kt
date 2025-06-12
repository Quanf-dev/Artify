package com.example.camera.ui.preview

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.camera.R
import java.io.File

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        val imageView: ImageView = findViewById(R.id.preview_image)
        val backButton: ImageButton = findViewById(R.id.button_back)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                imageView.setImageURI(Uri.fromFile(imageFile))
            } else {
                finish() // Finish if the image file doesn't exist
            }
        } else {
            finish() // Finish if no path is provided
        }

        backButton.setOnClickListener {
            // Delete the temp file when going back
            if (imagePath != null) {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    imageFile.delete()
                }
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }
} 