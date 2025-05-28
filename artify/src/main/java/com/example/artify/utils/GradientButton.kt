package com.example.artify.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.example.artify.R

class GradientButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

    private val colors = intArrayOf(
        0xFF7B29CD.toInt(),
        0xFF870DD1.toInt(),
        0xFF5B30F0.toInt(),
        0xFF8054F2.toInt()
    )

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GradientButton)
        val cornerRadius = typedArray.getDimension(R.styleable.GradientButton_cornerRadius, 0f)
        typedArray.recycle()

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            colors
        ).apply {
            this.cornerRadius = cornerRadius
        }

        val rippleColor = Color.parseColor("#66FFFFFF") // màu ripple hơi trắng trong suốt
        val rippleDrawable = RippleDrawable(
            android.content.res.ColorStateList.valueOf(rippleColor),
            gradientDrawable, // nền button
            null // mask (null nghĩa ripple phủ hết background)
        )

        background = rippleDrawable
    }
}
