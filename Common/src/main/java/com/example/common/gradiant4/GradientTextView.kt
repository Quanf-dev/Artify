package com.example.common.gradiant4

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val colors = intArrayOf(
        "#7B29CD".toColorInt(),
        "#870DD1".toColorInt(),
        "#5B30F0".toColorInt(),
        "#8054F2".toColorInt()
    )

    private val positions = floatArrayOf(0f, 0.38f, 0.68f, 0.96f)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyGradient()
    }

    private fun applyGradient() {
        val width = paint.measureText(text.toString())
        val shader = LinearGradient(
            0f, 0f, width, 0f,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        invalidate()
    }

    init {
        // Tạo ripple nền trong suốt để có hiệu ứng khi nhấn
        val rippleColor = "#33000000".toColorInt() // ripple hơi đen trong suốt
        val rippleDrawable = RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            Color.TRANSPARENT.toDrawable(), // nền trong suốt
            null
        )
        background = rippleDrawable
        isClickable = true // cho phép nhận sự kiện click
        isFocusable = true
    }
}
