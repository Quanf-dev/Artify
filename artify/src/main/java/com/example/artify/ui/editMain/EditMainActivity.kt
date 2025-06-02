package com.example.artify.ui.editMain

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.artify.R
import java.io.File
import java.io.FileOutputStream

class EditMainActivity : AppCompatActivity() {
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val inputStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                // Lưu bitmap vào file tạm
                val file = File(cacheDir, "editing_image.png")
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                // Truyền path qua Intent sang PaintActivity
                val intent = Intent(this, com.example.artify.ui.paint.PaintActivity::class.java)
                intent.putExtra("image_path", file.absolutePath)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Ví dụ: Gọi pickImageLauncher khi cần chọn ảnh
        // pickImageLauncher.launch("image/*")
    }

    // Hàm này có thể gọi từ nút chọn ảnh
    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }
}