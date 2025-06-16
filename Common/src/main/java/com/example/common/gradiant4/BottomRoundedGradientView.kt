package com.example.common.gradiant4

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import com.example.common.R

class BottomRoundedGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

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

        // Chỉ bo góc dưới (bottom left, bottom right)
        val radii = floatArrayOf(
            0f, 0f,    // top-left
            0f, 0f,    // top-right
            cornerRadius, cornerRadius,  // bottom-right
            0f, 0f // bottom-left
        )

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            colors
        ).apply {
            this.cornerRadii = radii
        }

        background = gradientDrawable
    }
}
