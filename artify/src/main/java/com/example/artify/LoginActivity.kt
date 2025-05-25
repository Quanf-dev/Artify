package com.example.artify

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.artify.utils.FullGradientDrawable
import com.example.artify.utils.GradientDotDrawable
import com.example.artify.utils.dpToPx

class LoginActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var emailEditText: EditText
    private lateinit var btnSignIn: Button


    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loginv2)

        backButton = findViewById(R.id.iv_back_button)
        emailEditText = findViewById(R.id.edt_input_email)
        btnSignIn = findViewById(R.id.btn_sign_in)

        backButton.setOnClickListener {
            finish()
        }
        val gradientBorder = GradientDotDrawable(
            height = dpToPx(2),
            cornerRadius = dpToPx(10).toFloat()
        )
        val gradientBackgroud = FullGradientDrawable(
            cornerRadius = dpToPx(50).toFloat()
        )
        emailEditText.background = gradientBorder
        btnSignIn.backgroundTintList = null
        btnSignIn.background = gradientBackgroud

// Set touch listener để bắt sự kiện chạm vào drawableEnd (icon mắt)
        val edtPassword = findViewById<EditText>(R.id.edt_input_password)
        edtPassword.background = gradientBorder
        var isPasswordVisible = false

        edtPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableStart = 0 // drawableStart index
                // Kiểm tra vị trí chạm nằm trong khoảng drawableStart (bên trái)
                if (event.rawX <= (edtPassword.left + edtPassword.compoundDrawables[drawableStart].bounds.width() + edtPassword.paddingStart)) {
                    if (isPasswordVisible) {
                        // Ẩn mật khẩu
                        edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        edtPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_eye, 0, 0, 0)
                        isPasswordVisible = false
                    } else {
                        // Hiển thị mật khẩu
                        edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        edtPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_eye_close, 0, 0, 0)
                        isPasswordVisible = true
                    }
                    edtPassword.setSelection(edtPassword.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}
