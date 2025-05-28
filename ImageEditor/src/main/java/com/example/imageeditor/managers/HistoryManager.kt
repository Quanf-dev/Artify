package com.example.imageeditor.managers

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi

// Represents a state in history, typically the bitmap of a layer and the layer's ID
data class LayerBitmapState(val layerId: String, val bitmap: Bitmap)

class HistoryManager {
    private val undoStack = mutableListOf<LayerBitmapState>()
    private val redoStack = mutableListOf<LayerBitmapState>()

    private val maxHistorySize = 30 // Limit the history size

    fun saveState(layerId: String, bitmap: Bitmap) {
        // Crucial: Make a copy of the bitmap to store, not a reference
        val bitmapCopy = bitmap.config?.let { bitmap.copy(it, true) }
        val state = bitmapCopy?.let { LayerBitmapState(layerId, it) }

        undoStack.add(state!!)
        redoStack.clear() // Any new action clears the redo stack

        // Maintain max history size
        if (undoStack.size > maxHistorySize) {
            undoStack.firstOrNull()?.bitmap?.recycle() // Recycle the oldest bitmap
            undoStack.removeAt(0)
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    // Returns the state that was undone, or null if nothing to undo
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun undo(currentLayerState: LayerBitmapState): LayerBitmapState? {
        if (!canUndo()) return null

        val lastState = undoStack.removeLast()
        // currentLayerState is the state *after* the undone action. Add it to redo stack.
        redoStack.add(currentLayerState.copy(bitmap = currentLayerState.bitmap.copy(
            currentLayerState.bitmap.config!!, true)))
        return lastState // This is the bitmap state to restore
    }

    // Returns the state to be redone, or null if nothing to redo
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun redo(currentLayerState: LayerBitmapState): LayerBitmapState? {
        if (!canRedo()) return null

        val nextState = redoStack.removeLast()
        // currentLayerState is the state *before* the redone action. Add it to undo stack.
        currentLayerState.bitmap.config?.let { undoStack.add(currentLayerState.copy(bitmap = currentLayerState.bitmap.copy(it, true))) }
        return nextState // This is the bitmap state to restore
    }

    fun clearHistory() {
        undoStack.forEach { it.bitmap.recycle() }
        undoStack.clear()
        redoStack.forEach { it.bitmap.recycle() }
        redoStack.clear()
    }
} 