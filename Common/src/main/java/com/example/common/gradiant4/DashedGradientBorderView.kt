package com.example.common.gradiant4

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout

class DashedGradientBorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val strokeWidth = dpToPx(3f)
    private val dashWidth = dpToPx(8f)
    private val dashGap = dpToPx(12f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@DashedGradientBorderView.strokeWidth
        pathEffect = DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
    }

    private val colors = intArrayOf(
        Color.parseColor("#7B29CD"),
        Color.parseColor("#870DD1"),
        Color.parseColor("#5B30F0"),
        Color.parseColor("#8054F2")
    )

    private val positions = floatArrayOf(0f, 0.38f, 0.68f, 0.96f)

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val rect = RectF(
            strokeWidth / 2,
            strokeWidth / 2,
            width - strokeWidth / 2,
            height - strokeWidth / 2
        )

        val shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, positions, Shader.TileMode.CLAMP
        )
        paint.shader = shader

        val cornerRadii = floatArrayOf(
            dpToPx(16f), dpToPx(16f), // top-left
            dpToPx(4f), dpToPx(4f),   // top-right
            dpToPx(16f), dpToPx(16f), // bottom-right
            dpToPx(4f), dpToPx(4f)    // bottom-left
        )

        val path = Path().apply {
            addRoundRect(rect, cornerRadii, Path.Direction.CW)
        }

        canvas.drawPath(path, paint)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
