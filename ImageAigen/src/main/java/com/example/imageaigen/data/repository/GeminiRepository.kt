package com.example.imageaigen.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.imageaigen.data.model.GeminiResponse
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagenGenerationConfig
import com.google.firebase.ai.type.PublicPreviewAPI
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
            responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
        }

    )

    @OptIn(PublicPreviewAPI::class)
    val GenImageModel = Firebase.ai(backend = GenerativeBackend.googleAI()).imagenModel(
        modelName = "imagen-3.0-generate-002",
        generationConfig = ImagenGenerationConfig(numberOfImages = 5)
    )

    @OptIn(PublicPreviewAPI::class)
    val AnimeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).imagenModel(
        modelName = "imagen-3.0-generate-002",
        generationConfig = ImagenGenerationConfig(numberOfImages = 4)
    )


    /**
     * Generate image from text prompt
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun generateImageFromText(prompt: String): List<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val imageResponse = AnimeModel.generateImages(prompt)
            imageResponse.images.mapNotNull { it.asBitmap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Edit image with text instructions
     */
    suspend fun removeBackgroundImage(bitmap: Bitmap): GeminiResponse {
        return try {
            val content = content {
                image(bitmap)
                text("Remove the background of the image, keeping only the main subject in full detail. The background should be completely transparent, with clean edges around the subject and no artifacts.")
            }
            val response = model.generateContent(content)
            val generatedImage =
                response.candidates.first().content.parts.firstNotNullOfOrNull { it.asImageOrNull() }
            GeminiResponse(
                bitmap = generatedImage,
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

    suspend fun CartoonImage(image: Bitmap): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            val imagePrompt: Content = content {
                image(image)
                text("Convert this photo into a Disney-style 3D cartoon illustration, keeping the original composition, camera angle, subject placement, proportions, and spatial layout exactly the same. Apply vibrant colors, smooth gradients, soft lighting, and expressive 3D character rendering in the style of modern Disney animation. Preserve all original element positions—transform only the visual style into a polished, cinematic 3D cartoon look.")
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
    suspend fun startChatAndEditImage(
        image: Bitmap,
        promptText: String
    ): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            val prompt = content {
                image(image)
                text(promptText)
            }

            val response = model.generateContent(prompt)

            val generatedImage = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstNotNullOfOrNull { it.asImageOrNull() }

            GeminiResponse(
                bitmap = generatedImage,
                isError = false
            )
        } catch (e: Exception) {
            GeminiResponse(
                bitmap = null,
                isError = true,
                errorMessage = e.message
            )
        }
    }


    /**
     * Generate anime images from text prompt using AnimeModel (multiple images)
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun generateAnimeImages(prompt: String): List<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val imageResponse = AnimeModel.generateImages(prompt)
            imageResponse.images.mapNotNull { it.asBitmap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Generate multiple images from text prompt (for GenerateImageViewModel)
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun generateImagesFromText(prompt: String): List<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val imageResponse = GenImageModel.generateImages(prompt)
            imageResponse.images.mapNotNull { it.asBitmap() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
