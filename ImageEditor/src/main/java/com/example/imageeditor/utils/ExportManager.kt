package com.example.imageeditor.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ExportManager(private val context: Context) {
    
    suspend fun saveBitmapToFile(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                saveBitmapToStream(bitmap, out, format, quality)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun saveBitmapToUri(bitmap: Bitmap, uri: Uri, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                saveBitmapToStream(bitmap, out, format, quality)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun saveBitmapToStream(bitmap: Bitmap, outputStream: OutputStream, format: Bitmap.CompressFormat, quality: Int) {
        bitmap.compress(format, quality, outputStream)
        outputStream.flush()
    }
}