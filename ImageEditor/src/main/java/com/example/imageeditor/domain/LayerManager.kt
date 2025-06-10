package com.example.imageeditor.domain

import android.graphics.Canvas

class LayerManager {
    private val layers = mutableListOf<Layer>()
    private var activeLayer: Layer? = null

    init {
        // Create default layer
        val defaultLayer = Layer()
        layers.add(defaultLayer)
        activeLayer = defaultLayer
    }

    fun addLayer(): Layer {
        val layer = Layer()
        layers.add(layer)
        return layer
    }

    fun removeLayer(layer: Layer) {
        if (layers.size > 1) {
            layers.remove(layer)
            if (activeLayer == layer) {
                activeLayer = layers.lastOrNull()
            }
        }
    }

    fun getActiveLayer(): Layer? = activeLayer

    fun setActiveLayer(layer: Layer) {
        if (layers.contains(layer)) {
            activeLayer = layer
        }
    }

    fun getLayers(): List<Layer> = layers.toList()

    fun draw(canvas: Canvas) {
        layers.forEach { it.draw(canvas) }
    }

    fun clear() {
        layers.forEach { it.clear() }
    }
} 