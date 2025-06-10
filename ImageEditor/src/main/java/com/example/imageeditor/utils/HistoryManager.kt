package com.example.imageeditor.utils

import com.example.imageeditor.tools.base.DrawableItem

class HistoryManager(private val maxHistorySize: Int = 50) {
    private val undoStack = mutableListOf<DrawableItem>()
    private val redoStack = mutableListOf<DrawableItem>()
    
    fun addHistoryItem(item: DrawableItem) {
        undoStack.add(item)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear() // Clear redo stack when new action is performed
    }
    
    fun undo(): DrawableItem? {
        if (undoStack.isEmpty()) return null
        
        val item = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(item)
        return item
    }
    
    fun redo(): DrawableItem? {
        if (redoStack.isEmpty()) return null
        
        val item = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(item)
        return item
    }
    
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}