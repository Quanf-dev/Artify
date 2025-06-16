package com.example.imageaigen.data.model

import android.graphics.Bitmap

/**
 * Data class to hold the response from Gemini AI
 */
data class GeminiResponse(
    val bitmap: Bitmap? = null,
    val text: String? = null,
    val isError: Boolean = false,
    val errorMessage: String? = null
)