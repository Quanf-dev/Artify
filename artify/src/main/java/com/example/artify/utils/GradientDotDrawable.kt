package com.example.artify.utils

import android.graphics.*
import android.graphics.drawable.Drawable

class GradientDotDrawable(
    private val width: Int? = null,             // nullable: nếu null sẽ dùng bounds.width()
    private val height: Int,
    private val cornerRadius: Float = 10f
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
        val actualWidth = width ?: bounds.width()         // nếu width null thì dùng bounds.width()
        val actualLeft = 0f
        val actualRight = actualWidth.toFloat()
        val actualTop = bounds.height() - height.toFloat()
        val actualBottom = bounds.height().toFloat()

        val shader = LinearGradient(
            actualLeft, actualTop, actualRight, actualTop,
            colors, positions, Shader.TileMode.CLAMP
        )
        paint.shader = shader

        val rectF = RectF(actualLeft, actualTop, actualRight, actualBottom)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
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

    override fun getIntrinsicWidth(): Int = width ?: -1 // -1 nghĩa là không xác định → sẽ dùng bounds
    override fun getIntrinsicHeight(): Int = height
}
