package com.example.artify.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2

class CustomDotsIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var dotsCount = 0
    private val dots = mutableListOf<ImageView>()
    private var viewPager: ViewPager2? = null

    private val dotSizeActiveWidth = dpToPx(32)
    private val dotSizeActiveHeight = dpToPx(6)
    private val dotSizeInactive = dpToPx(8)
    private val dotMargin = dpToPx(4)

    private var selectedDotDrawable: Drawable? = null
    private var unselectedDotDrawable: Drawable? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun setSelectedDotDrawable(drawable: Drawable) {
        selectedDotDrawable = drawable
    }

    fun setUnselectedDotDrawable(drawable: Drawable) {
        unselectedDotDrawable = drawable
    }

    fun setupWithViewPager(viewPager: ViewPager2, itemCount: Int) {
        this.viewPager = viewPager
        dotsCount = itemCount
        createDots()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setCurrentDot(position)
            }
        })
    }

    private fun createDots() {
        if (selectedDotDrawable == null || unselectedDotDrawable == null) {
            throw IllegalStateException("You must set both selected and unselected drawables before calling setupWithViewPager.")
        }

        removeAllViews()
        dots.clear()

        for (i in 0 until dotsCount) {
            val dot = ImageView(context)
            dot.setImageDrawable(unselectedDotDrawable)
            val params = LayoutParams(dotSizeInactive, dotSizeInactive)
            params.setMargins(dotMargin, 0, dotMargin, 0)
            dot.layoutParams = params
            dot.alpha = 0.5f

            dot.setOnClickListener {
                viewPager?.setCurrentItem(i, true)
            }

            dots.add(dot)
            addView(dot)
        }
    }

    fun setCurrentDot(position: Int) {
        for (i in dots.indices) {
            val dot = dots[i]
            if (i == position) {
                dot.setImageDrawable(selectedDotDrawable)
                animateDot(
                    dot,
                    dot.width.takeIf { it > 0 } ?: dotSizeInactive, // width hiện tại hoặc 6dp mặc định
                    dotSizeActiveWidth,
                    dot.alpha,
                    1f
                )
                // Chiều cao giữ cố định 6dp khi active
                val params = dot.layoutParams
                params.height = dotSizeActiveHeight
                dot.layoutParams = params
            } else {
                dot.setImageDrawable(unselectedDotDrawable)
                animateDot(
                    dot,
                    dot.width.takeIf { it > 0 } ?: dotSizeActiveWidth, // width hiện tại hoặc 32dp mặc định
                    dotSizeInactive,
                    dot.alpha,
                    0.5f
                )
                // Chiều cao inactive là 6dp (hình tròn)
                val params = dot.layoutParams
                params.height = dotSizeInactive
                dot.layoutParams = params
            }
        }
    }

    private fun animateDot(dot: ImageView, fromWidth: Int, toWidth: Int, fromAlpha: Float, toAlpha: Float) {
        val widthAnimator = ValueAnimator.ofInt(fromWidth, toWidth).apply {
            duration = 300
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val params = dot.layoutParams
                params.width = value
                // Chiều cao giữ nguyên đã set bên ngoài
                dot.layoutParams = params
            }
        }

        val alphaAnimator = ObjectAnimator.ofFloat(dot, "alpha", fromAlpha, toAlpha).apply {
            duration = 300
        }

        AnimatorSet().apply {
            playTogether(widthAnimator, alphaAnimator)
            start()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
