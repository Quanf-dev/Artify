package com.example.imageeditor.models

// Represents an action performed on the canvas, used for undo/redo
sealed class DrawingAction {
    // Base class for drawing actions
}

// Example: An action that adds a shape to a specific layer
data class AddShapeAction(
    val layerId: String,
    val shape: Shape
) : DrawingAction()

// Example: An action that modifies a layer's properties (e.g., visibility, opacity)
data class ModifyLayerAction(
    val layerId: String,
    val oldLayerState: Layer, // Store the state before modification for undo
    val newLayerState: Layer // Store the new state for redo/apply
) : DrawingAction()

// Add more actions as needed, e.g., DeleteLayerAction, MoveLayerAction, etc. 