package com.example.artify.utils

// File: NavigationUtil.kt
import android.app.Activity
import android.content.Context
import android.content.Intent

fun Context.navigate(to: Class<out Activity>) {
    startActivity(Intent(this, to))
}
