package com.example.common.gradiant4

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.LinearLayout

class LinearGradientBorder @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val borderWidth = 6f  // độ dày viền
    private val cornerRadius = 10f * resources.displayMetrics.density // 10dp

    private val gradientColors = intArrayOf(
        0xFF7B29CD.toInt(),
        0xFF870DD1.toInt(),
        0xFF5B30F0.toInt(),
        0xFF8054F2.toInt()
    )

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }

    init {
        setWillNotDraw(false)
        setPadding(
            (paddingLeft + borderWidth).toInt(),
            (paddingTop + borderWidth).toInt(),
            (paddingRight + borderWidth).toInt(),
            (paddingBottom + borderWidth).toInt()
        )
        background = null // xóa nền mặc định để tự vẽ
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rect = RectF(
            borderWidth / 2,
            borderWidth / 2,
            width - borderWidth / 2,
            height - borderWidth / 2
        )

        if (isSelected) {
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                gradientColors, null, Shader.TileMode.CLAMP
            )
            borderPaint.shader = gradient
        } else {
            borderPaint.shader = null
            borderPaint.color = Color.WHITE
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }
}
