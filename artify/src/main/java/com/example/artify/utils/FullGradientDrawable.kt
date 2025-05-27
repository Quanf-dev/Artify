package com.example.artify.utils

import android.graphics.*
import android.graphics.drawable.Drawable

class FullGradientDrawable(
    private val cornerRadius: Float = 0f
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val colors = intArrayOf(
        Color.parseColor("#7B29CD"),
        Color.parseColor("#870DD1"),
        Color.parseColor("#5B30F0"),
        Color.parseColor("#8054F2")
    )

    private val positions = floatArrayOf(0f, 0.38f, 0.68f, 0.96f)

    override fun draw(canvas: Canvas) {
        val rect = RectF(bounds)

        val shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.top, // trái → phải
            colors, positions, Shader.TileMode.CLAMP
        )
        paint.shader = shader

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    // 👉 Thêm đoạn này để kích hoạt kích thước tự động
    override fun getIntrinsicWidth(): Int = -1
    override fun getIntrinsicHeight(): Int = -1
}
