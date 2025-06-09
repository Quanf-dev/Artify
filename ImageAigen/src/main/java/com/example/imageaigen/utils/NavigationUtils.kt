package com.example.imageaigen.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.example.imageaigen.ui.edit.EditImageActivity
import com.example.imageaigen.ui.preview.PreviewImageActivity
import java.io.File

object NavigationUtils {

    fun navigateToPreview(context: Context, bitmap: Bitmap) {
        val uri = ImageUtils.createImageUri(context, bitmap, "preview")
        val intent = Intent(context, PreviewImageActivity::class.java).apply {
            putExtra("image_uri", uri)
        }
        context.startActivity(intent)
    }

    fun navigateToPreview(context: Context, bitmaps: List<Bitmap>) {
        // Create cache directory if it doesn't exist
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // Clear previous images
        cacheDir.listFiles()?.forEach { it.delete() }

        // Save all bitmaps to cache and collect URIs
        val imageUris = ArrayList<Uri>()
        bitmaps.forEachIndexed { index, bitmap ->
            val uri = ImageUtils.createImageUri(context, bitmap, "preview_$index")
            imageUris.add(uri)
        }

        // Start activity with image URIs
        val intent = Intent(context, PreviewImageActivity::class.java).apply {
            putParcelableArrayListExtra("image_uris", imageUris)
        }
        context.startActivity(intent)
    }

    fun navigateToEdit(context: Context, bitmap: Bitmap) {
        val uri = ImageUtils.createImageUri(context, bitmap, "edit")
        val intent = Intent(context, EditImageActivity::class.java).apply {
            putExtra("image_uri", uri)
        }
        context.startActivity(intent)
    }

    fun shareImage(context: Context, bitmap: Bitmap) {
        val uri = ImageUtils.createImageUri(context, bitmap, "share")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    fun viewImage(context: Context, bitmap: Bitmap) {
        val uri = ImageUtils.createImageUri(context, bitmap, "view")
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(viewIntent)
    }
}