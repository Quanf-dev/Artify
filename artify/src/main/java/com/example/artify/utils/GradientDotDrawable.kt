package com.example.artify.utils

import android.graphics.*
import android.graphics.drawable.Drawable

class GradientDotDrawable(
    private val width: Int,
    private val height: Int,
    private val cornerRadius: Float = 10f  // radius tròn góc (px)
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
        val bounds = bounds

        val shader = LinearGradient(
            0f, 0f,
            bounds.width().toFloat(), 0f,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader

        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getIntrinsicWidth(): Int = width

    override fun getIntrinsicHeight(): Int = height
}
