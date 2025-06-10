package com.example.common.gradiant4

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.common.R

class GradientConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val colors = intArrayOf(
        0xFF7B29CD.toInt(),
        0xFF870DD1.toInt(),
        0xFF5B30F0.toInt(),
        0xFF8054F2.toInt()
    )

    private var cornerRadius = 24f * resources.displayMetrics.density // default 24dp

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.GradientConstraintLayout)
            cornerRadius = typedArray.getDimension(R.styleable.GradientConstraintLayout_cornerRadius, cornerRadius)
            typedArray.recycle()
        }

        // Set background gradient + ripple bằng drawable helper
        background = createGradientRippleDrawable(colors, cornerRadius)
    }

    private fun createGradientRippleDrawable(
        colors: IntArray,
        cornerRadius: Float,
        rippleColorHex: String = "#33000000"
    ): RippleDrawable {
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            colors
        ).apply {
            this.cornerRadius = cornerRadius
        }

        val rippleColor = Color.parseColor(rippleColorHex)
        return RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            gradientDrawable,
            null
        )
    }
}
