package com.example.artify

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.artify.databinding.ActivityPaintzBinding
import com.example.artify.databinding.DialogPaintSettingsBinding
import com.example.artify.databinding.ItemToolbarPaintBinding
import com.mikepenz.iconics.IconicsDrawable
import eightbitlab.com.blurview.RenderScriptBlur

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaintzBinding
    private lateinit var toolbarBinding: ItemToolbarPaintBinding
    private var paintSettingsDialog: Dialog? = null
    private var dialogBinding: DialogPaintSettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPaintzBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize toolbar binding
        toolbarBinding = ItemToolbarPaintBinding.bind(binding.root.findViewById(R.id.toolbarPaint))

        setupBlurView()
        setupPaintSettingsDialog()
        setupClickListeners()
    }

    private fun setupBlurView() {
        val radius = 1f
        // Lấy view cha chứa nội dung chính
        val parentView = binding.root.findViewById<ViewGroup>(R.id.mainContent)
        
        binding.blurView.setupWith(parentView, RenderScriptBlur(this))
            .setFrameClearDrawable(window.decorView.background)
            .setBlurRadius(radius)

        // Thêm click listener cho overlay container
        binding.overlayContainer.setOnClickListener {
            paintSettingsDialog?.dismiss()
        }
    }

    private fun setupPaintSettingsDialog() {
        paintSettingsDialog = Dialog(this, android.R.style.Theme_Material_Light_NoActionBar)
        dialogBinding = DialogPaintSettingsBinding.inflate(layoutInflater)
        paintSettingsDialog?.setContentView(dialogBinding?.root!!)

        // Set dialog position and size
        paintSettingsDialog?.window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        // Cho phép đóng dialog khi click bên ngoài
        paintSettingsDialog?.setCanceledOnTouchOutside(true)
        paintSettingsDialog?.setCancelable(true)

        // Make dialog dismissible when clicking outside
        paintSettingsDialog?.setOnDismissListener {
            binding.overlayContainer.visibility = View.GONE
        }

        toolbarBinding.btnFormatColorReset.setOnClickListener {
            it.isSelected = !it.isSelected

            val newIconRes = if (it.isSelected) R.drawable.ic_raindrop else R.drawable.ic_format_color_reset
            toolbarBinding.btnFormatColorReset.setImageResource(newIconRes)
        }


        // Setup close buttons
        dialogBinding?.btnCloseDialog?.setOnClickListener {
            paintSettingsDialog?.dismiss()
        }
        dialogBinding?.btnCloseDialogOpacity?.setOnClickListener {
            paintSettingsDialog?.dismiss()
        }

        // Setup SeekBars
        dialogBinding?.seekBarLineWidth?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.paintEditorView.setStrokeWidth(progress.toFloat().coerceAtLeast(1f))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding?.seekBarOpacity?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.paintEditorView.setOpacity(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupClickListeners() {
        toolbarBinding.btnFilterList.setOnClickListener {
            showLineWidthDialog()
        }

        toolbarBinding.btnBlurLinear.setOnClickListener {
            showOpacityDialog()
        }
    }

    private fun showLineWidthDialog() {
        dialogBinding?.apply {
            containerLineWidth.visibility = View.VISIBLE
            containerOpacity.visibility = View.GONE
        }
        binding.overlayContainer.visibility = View.VISIBLE
        paintSettingsDialog?.show()
    }

    private fun showOpacityDialog() {
        dialogBinding?.apply {
            containerLineWidth.visibility = View.GONE
            containerOpacity.visibility = View.VISIBLE
        }
        binding.overlayContainer.visibility = View.VISIBLE
        paintSettingsDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        paintSettingsDialog?.dismiss()
        paintSettingsDialog = null
        dialogBinding = null
    }
}
