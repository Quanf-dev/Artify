package com.example.imageaigen.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.imageaigen.data.model.GeminiResponse
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.asImageOrNull
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository(context: Context) {

    // Sử dụng Firebase AI với backend là Google AI Gemini
    val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = "gemini-2.0-flash-preview-image-generation",
        // Configure the model to respond with text and images
        generationConfig = generationConfig {
            responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE) }
    )

    /**
     * Generate image from text prompt
     */
    suspend fun generateImageFromText(prompt: String): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            val textPrompt: Content = content {
                text(prompt)
            }

            val response = model.generateContent(prompt)
            val generatedText = response.text

            val generatedImage = response.candidates.firstOrNull()
                ?.content?.parts?.firstNotNullOfOrNull { it.asImageOrNull() }

            GeminiResponse(
                bitmap = generatedImage,
                text = generatedText,
                isError = false
            )
        } catch (e: Exception) {
            GeminiResponse(
                bitmap = null,
                text = null,
                isError = true,
                errorMessage = e.message
            )
        }
    }

    /**
     * Edit image with text instructions
     */
    suspend fun editImage(image: Bitmap, prompt: String): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            val imagePrompt: Content = content {
                image(image)
                text(prompt)
            }

            val response = model.generateContent(imagePrompt)
            val generatedText = response.text

            val generatedImage = response.candidates.firstOrNull()
                ?.content?.parts?.firstNotNullOfOrNull { it.asImageOrNull() }

            GeminiResponse(
                bitmap = generatedImage,
                text = generatedText,
                isError = false
            )
        } catch (e: Exception) {
            GeminiResponse(
                bitmap = null,
                text = null,
                isError = true,
                errorMessage = e.message
            )
        }
    }

    /**
     * Start a chat session for iterative image editing
     */
    suspend fun startChatAndEditImage(image: Bitmap, prompt: String): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            val chat = model.startChat()

            val initialPrompt = content {
                image(image)
                text(prompt)
            }

            val response = chat.sendMessage(initialPrompt)
            val generatedText = response.text

            val generatedImage = response.candidates.firstOrNull()
                ?.content?.parts?.firstNotNullOfOrNull { it.asImageOrNull() }

            GeminiResponse(
                bitmap = generatedImage,
                text = generatedText,
                isError = false
            )
        } catch (e: Exception) {
            GeminiResponse(
                bitmap = null,
                text = null,
                isError = true,
                errorMessage = e.message
            )
        }
    }
}
