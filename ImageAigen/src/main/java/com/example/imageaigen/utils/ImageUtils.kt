package com.example.imageaigen.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String = "image"): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "${prefix}_${timestamp}.jpg"
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, filename)
        val outputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }

    fun createImageUri(context: Context, bitmap: Bitmap, prefix: String = "image"): Uri {
        return saveBitmapToCache(context, bitmap, prefix)
    }
} 