package com.example.artify.ui.sticker

import android.os.Bundle
import android.view.View
import com.example.artify.R
import com.example.artify.databinding.ActivityStickerBinding
import com.example.artify.databinding.ItemToolbarEditMainBinding
import com.example.artify.ui.editbase.BaseEditActivity
import com.example.imageeditor.ui.views.ImageStickerView

class StickerActivity : BaseEditActivity<ActivityStickerBinding>() {

    private lateinit var imageStickerView: ImageStickerView
    private lateinit var toolbarBinding: ItemToolbarEditMainBinding

    override fun inflateBinding(): ActivityStickerBinding {
        return ActivityStickerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        toolbarBinding = ItemToolbarEditMainBinding.bind(binding.root.findViewById(R.id.tbSticker))
        imageStickerView = binding.imageStickerView
        

        toolbarBinding.ivDone.setOnClickListener {
            val editedBitmap = imageStickerView.getEditedBitmap()
            returnEditedImage(editedBitmap)
        }

        // Nhận ảnh đầu vào đồng bộ
        getInputBitmap(
            onBitmapReady = { bitmap ->
                imageStickerView.setImageBitmap(bitmap)
                currentImageBitmap = bitmap
                
            },
            onError = {
            }
        )
        with(toolbarBinding) {
            ivRedo.visibility = View.GONE
            ivUndo.visibility = View.GONE
        }
    }


}