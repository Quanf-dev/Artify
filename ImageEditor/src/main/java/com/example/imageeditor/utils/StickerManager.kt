package com.example.imageeditor.utils

import android.content.Context
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Manager class to handle sticker loading and categorization
 */
class StickerManager(private val context: Context) {

    private val stickerCategories = mutableListOf<StickerCategory>()

    // Map for defining category names and icons based on category IDs from data_Microsoft3D
    private val emojiDataSourceCategoryMap = mapOf(
        "1" to ("Smileys & People" to "😀"),
        "2" to ("Animals & Nature" to "🐶"),
        "3" to ("Food & Drink" to "🍔"),
        "4" to ("Activities" to "⚽"),
        "5" to ("Travel & Places" to "✈️"),
        "6" to ("Objects" to "💡"),
        "7" to ("Symbols" to "❤️"),
        "8" to ("Flags" to "🏳️")
    )

    /**
     * Initialize sticker data by loading from assets
     */
    suspend fun initialize() {
        loadStickersFromAssets()
    }

    /**
     * Load stickers from assets data_Microsoft3D file
     */
    private suspend fun loadStickersFromAssets() = withContext(Dispatchers.IO) {
        stickerCategories.clear()
        val rawStickersByCategory = mutableMapOf<String, MutableList<Sticker>>()

        try {
            context.assets.open("stickers/data_Microsoft3D").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split('|')
                        if (parts.size >= 6) {
                            val emojiChar = parts[0]
                            val emojiName = parts[1]
                            val categoryId = parts[5]

                            val sticker = Sticker(emojiName, emojiChar, true)
                            rawStickersByCategory.getOrPut(categoryId) { mutableListOf() }.add(sticker)
                        }
                    }
                }
            }

            rawStickersByCategory.forEach { (categoryId, stickers) ->
                emojiDataSourceCategoryMap[categoryId]?.let { (categoryName, categoryIcon) ->
                    stickerCategories.add(
                        StickerCategory(
                            name = categoryName,
                            iconPath = categoryIcon,
                            stickers = stickers
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get all sticker categories
     */
    fun getStickerCategories(): List<StickerCategory> = stickerCategories

    /**
     * Get stickers for a specific category
     */
    fun getStickersForCategory(categoryIndex: Int): List<Sticker> {
        return if (categoryIndex in 0 until stickerCategories.size) {
            stickerCategories[categoryIndex].stickers
        } else {
            emptyList()
        }
    }

    /**
     * Load a sticker as a drawable
     */
    fun loadStickerDrawable(sticker: Sticker): Drawable? {
        return try {
            if (sticker.isEmoji) {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 128f
                    color = android.graphics.Color.BLACK
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                val bounds = android.graphics.Rect()
                paint.getTextBounds(sticker.path, 0, sticker.path.length, bounds)

                val width = if (bounds.width() <= 0) 1 else bounds.width()
                val height = if (bounds.height() <= 0) 1 else bounds.height()

                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawText(sticker.path, 0f, -bounds.top.toFloat(), paint)
                android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
            } else {
                val inputStream = context.assets.open(sticker.path)
                Drawable.createFromStream(inputStream, null)
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Data class to represent a sticker category
 */
data class StickerCategory(
    val name: String,
    val iconPath: String,
    val stickers: List<Sticker>
)

/**
 * Data class to represent a sticker
 */
data class Sticker(
    val id: String,
    val path: String,
    val isEmoji: Boolean = false
) 