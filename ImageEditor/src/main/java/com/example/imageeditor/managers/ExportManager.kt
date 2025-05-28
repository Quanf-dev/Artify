package com.example.imageeditor.managers

import android.graphics.Bitmap
import android.content.Context
// import android.os.Environment // Not directly used in current file logic
import com.example.imageeditor.models.Layer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.util.Base64 // For bitmap to string
import android.graphics.BitmapFactory

class ExportManager(private val context: Context) {

    enum class ImageFormat {
        PNG, JPG
    }

    suspend fun saveBitmapToFile(bitmap: Bitmap, file: File, format: ImageFormat = ImageFormat.PNG, quality: Int = 100): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                FileOutputStream(file).use { fos ->
                    val success = when (format) {
                        ImageFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, quality, fos) // quality is ignored for PNG
                        ImageFormat.JPG -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), fos)
                    }
                    fos.flush()
                    success
                }
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }

    // --- Project Save/Load (JSON) ---

    // Define a structure for JSON serialization
    private data class ProjectData(
        val width: Int,
        val height: Int,
        val layers: List<LayerData>,
        val selectedLayerId: String?
    )

    private data class LayerData(
        val id: String,
        val name: String,
        val bitmapBase64: String, // Store bitmap as Base64 string
        var isVisible: Boolean,
        var opacity: Float
    )

    suspend fun saveProject(projectFile: File, layerManager: LayerManager): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val layersData = layerManager.allLayers.mapNotNull { layer ->
                    // Convert bitmap to Base64 string
                    val outputStream = java.io.ByteArrayOutputStream()
                    layer.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val bitmapBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                    LayerData(layer.id, layer.name, bitmapBase64, layer.isVisible, layer.opacity)
                }

                val projectData = ProjectData(
                    width = layerManager.allLayers.firstOrNull()?.bitmap?.width ?: 0, // Assuming all layers have same dimensions
                    height = layerManager.allLayers.firstOrNull()?.bitmap?.height ?: 0,
                    layers = layersData,
                    selectedLayerId = layerManager.currentLayer?.id
                )

                val jsonObject = JSONObject()
                jsonObject.put("width", projectData.width)
                jsonObject.put("height", projectData.height)
                jsonObject.put("selectedLayerId", projectData.selectedLayerId ?: JSONObject.NULL)

                val jsonLayersArray = JSONArray()
                projectData.layers.forEach { layerData ->
                    val jsonLayer = JSONObject()
                    jsonLayer.put("id", layerData.id)
                    jsonLayer.put("name", layerData.name)
                    jsonLayer.put("bitmapBase64", layerData.bitmapBase64)
                    jsonLayer.put("isVisible", layerData.isVisible)
                    jsonLayer.put("opacity", layerData.opacity.toDouble()) // JSON numbers
                    jsonLayersArray.put(jsonLayer)
                }
                jsonObject.put("layers", jsonLayersArray)

                projectFile.writeText(jsonObject.toString(4)) // Indented JSON for readability
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Returns new LayerManager instance or null on failure
    suspend fun loadProject(projectFile: File): LayerManager? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = projectFile.readText()
                val jsonObject = JSONObject(jsonString)

                val width = jsonObject.getInt("width")
                val height = jsonObject.getInt("height")
                val selectedLayerId = jsonObject.optString("selectedLayerId", null)

                if (width == 0 || height == 0) return@withContext null // Invalid project data

                val loadedLayerManager = LayerManager(width, height) // Create new LM
                loadedLayerManager.clearAllLayersForLoad() // Helper to remove initial default layer

                val jsonLayersArray = jsonObject.getJSONArray("layers")
                for (i in 0 until jsonLayersArray.length()) {
                    val jsonLayer = jsonLayersArray.getJSONObject(i)
                    val id = jsonLayer.getString("id")
                    val name = jsonLayer.getString("name")
                    val bitmapBase64 = jsonLayer.getString("bitmapBase64")
                    val isVisible = jsonLayer.getBoolean("isVisible")
                    val opacity = jsonLayer.getDouble("opacity").toFloat()

                    // Convert Base64 string back to bitmap
                    val imageBytes = Base64.decode(bitmapBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    .copy(Bitmap.Config.ARGB_8888, true) // Ensure mutable

                    if (bitmap.width != width || bitmap.height != height) {
                        // Bitmap dimensions mismatch, handle error or attempt to resize
                        // For now, skip this layer or return null for project load failure
                        // Log.e("ExportManager", "Bitmap dimension mismatch for layer $name")
                        // To keep it simple, we will assume dimensions match for now
                    }

                    val layer = Layer(id, name, bitmap, isVisible, opacity)
                    loadedLayerManager.addLoadedLayer(layer) // Helper to add without making it current initially
                }

                if (selectedLayerId != null) {
                    loadedLayerManager.selectLayer(selectedLayerId)
                }
                
                loadedLayerManager.ensureBaseLayerIfEmpty() // If all layers failed to load, add a default

                loadedLayerManager
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

// Helper extension in LayerManager.kt would be needed:
/*
fun LayerManager.clearAllLayersForLoad() {
    layers.forEach { it.bitmap.recycle() } // recycle existing bitmaps
    layers.clear()
    currentSelectedLayerIndex = -1
}

fun LayerManager.addLoadedLayer(layer: Layer) {
    layers.add(layer)
    // Don't automatically select, let loadProject handle selection
}
fun LayerManager.ensureBaseLayerIfEmpty() {
    if (layers.isEmpty()) {
        addLayer("Background") // Assuming addLayer handles bitmap creation for new layer
    }
}
*/ 