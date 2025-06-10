package com.example.imageeditor.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.drawToBitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imageeditor.R
import com.example.imageeditor.ui.adapters.StickerAdapter
import com.example.imageeditor.ui.adapters.StickerCategoryAdapter
import com.example.imageeditor.utils.Sticker
import com.example.imageeditor.utils.StickerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import miaoyongjun.stickerview.StickerView

/**
 * View that handles sticker editing with StickerView
 */
class ImageStickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Views
    private lateinit var imageViewBackground: ImageView
    private lateinit var stickerView: StickerView
    private lateinit var recyclerViewCategories: RecyclerView
    private lateinit var recyclerViewStickers: RecyclerView

    // Adapters
    private lateinit var categoryAdapter: StickerCategoryAdapter
    private lateinit var stickerAdapter: StickerAdapter

    // Sticker manager
    private val stickerManager = StickerManager(context)

    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Background bitmap
    private var backgroundBitmap: Bitmap? = null

    // Listener for when the image changes
    private var onImageChangedListener: ((Bitmap?) -> Unit)? = null

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_sticker_editor, this, true)

        // Get views
        imageViewBackground = findViewById(R.id.imageViewBackground)
        stickerView = findViewById(R.id.stickerView)
        recyclerViewCategories = findViewById(R.id.recyclerViewCategories)
        recyclerViewStickers = findViewById(R.id.recyclerViewStickers)

        // Add layout listener to ensure StickerView is ready
        stickerView.post {
            // Configure StickerView after layout
            stickerView.minStickerSizeScale = 0.1f  // 10% of original size
        }

        // Initialize stickers in background
        coroutineScope.launch {
            stickerManager.initialize()
            setupCategoryAdapter()
        }

        // Chặn ScrollView cuộn khi thao tác trên StickerView
        stickerView.setOnTouchListener { v, event ->
            // Khi bắt đầu chạm vào stickerView thì chặn ScrollView cuộn
            v.parent.requestDisallowInterceptTouchEvent(true)
            false // vẫn cho StickerView xử lý tiếp
        }
    }

    /**
     * Configure the StickerView
     */

    /**
     * Set up the sticker category adapter
     */
    private fun setupCategoryAdapter() {
        val categories = stickerManager.getStickerCategories()
        if (categories.isEmpty()) return

        categoryAdapter = StickerCategoryAdapter(context, categories) { categoryIndex ->
            // Load stickers for selected category
            val stickers = stickerManager.getStickersForCategory(categoryIndex)
            updateStickerAdapter(stickers)
        }

        recyclerViewCategories.adapter = categoryAdapter

        // Load stickers for first category
        val stickers = stickerManager.getStickersForCategory(0)
        setupStickerAdapter(stickers)
    }

    /**
     * Set up the sticker adapter
     */
    private fun setupStickerAdapter(stickers: List<Sticker>) {
        stickerAdapter = StickerAdapter(context, stickers, stickerManager) { sticker ->
            // Add sticker to StickerView
            addStickerToView(sticker)
        }

        // Set up grid layout for stickers
        val gridLayoutManager = recyclerViewStickers.layoutManager as? GridLayoutManager
        gridLayoutManager?.spanCount = 2

        recyclerViewStickers.adapter = stickerAdapter
    }

    /**
     * Update sticker adapter with new stickers
     */
    private fun updateStickerAdapter(stickers: List<Sticker>) {
        if (::stickerAdapter.isInitialized) {
            stickerAdapter.updateStickers(stickers)
        } else {
            setupStickerAdapter(stickers)
        }
    }

    /**
     * Add a sticker to the StickerView
     */
    private fun addStickerToView(sticker: Sticker) {
        coroutineScope.launch {
            try {
                val drawable = withContext(Dispatchers.IO) {
                    stickerManager.loadStickerDrawable(sticker)
                }

                if (drawable != null) {
                    // Scale down the drawable to a fixed size
                    val scaledDrawable = drawable.mutate()
                    val fixedSize = 150 // Fixed size in pixels
                    val ratio =
                        drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()

                    val width: Int
                    val height: Int

                    if (drawable.intrinsicWidth > drawable.intrinsicHeight) {
                        width = fixedSize
                        height = (fixedSize / ratio).toInt()
                    } else {
                        height = fixedSize
                        width = (fixedSize * ratio).toInt()
                    }

                    scaledDrawable.setBounds(0, 0, width, height)
                    stickerView.addSticker(scaledDrawable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Set the bitmap to be edited
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            backgroundBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            imageViewBackground.setImageBitmap(backgroundBitmap)
        }
    }

    /**
     * Get the edited bitmap with stickers
     */
    fun getEditedBitmap(): Bitmap? {
        if (backgroundBitmap == null) return null

        // Tạo một bitmap mới để giữ kết quả
        val result = backgroundBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Dán sticker (đã loại control) lên canvas
        val stickerOnlyBitmap = stickerView.saveSticker()
        canvas.drawBitmap(stickerOnlyBitmap, 0f, 0f, null)

        return result
    }



    /**
     * Notify that the image has changed
     */
    private fun notifyImageChanged() {
        onImageChangedListener?.invoke(getEditedBitmap())
    }

    fun removeAllStickers() {
        notifyImageChanged()
    }
}