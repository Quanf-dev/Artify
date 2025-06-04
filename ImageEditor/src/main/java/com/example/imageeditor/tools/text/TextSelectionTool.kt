package com.example.imageeditor.tools.text

import android.graphics.Canvas
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import com.example.imageeditor.domain.LayerManager
import com.example.imageeditor.tools.base.BaseTool
import com.example.imageeditor.tools.base.DrawableItem
import com.example.imageeditor.tools.base.TransformationCallback
import com.example.imageeditor.ui.views.PaintEditorView
import kotlin.math.atan2
import kotlin.math.sqrt

class TextSelectionTool(private val layerManager: LayerManager, private val view: View) : BaseTool(), TransformationCallback {
    private var selectedTextItem: TextDrawable? = null
    private var onTextSelectedListener: ((TextDrawable?) -> Unit)? = null
    private var onTextEditRequestListener: ((TextDrawable) -> Unit)? = null
    private var onTextDeleteRequestListener: ((TextDrawable) -> Unit)? = null
    
    private var initialTouchPoint: PointF? = null
    private var initialItemPosition: PointF? = null
    private var initialItemRotation: Float = 0f
    private var transformMode: TransformMode = TransformMode.NONE
    
    fun setOnTextSelectedListener(listener: (TextDrawable?) -> Unit) {
        onTextSelectedListener = listener
    }
    
    fun setOnTextEditRequestListener(listener: (TextDrawable) -> Unit) {
        onTextEditRequestListener = listener
    }
    
    fun setOnTextDeleteRequestListener(listener: (TextDrawable) -> Unit) {
        onTextDeleteRequestListener = listener
    }
    
    override fun onTouchStart(event: MotionEvent) {
        val touchPoint = PointF(event.x, event.y)
        initialTouchPoint = touchPoint
        
        // Lấy tất cả các đối tượng TextDrawable từ các lớp
        val allTextDrawables = layerManager.getLayers()
            .filter { it.isVisible }
            .flatMap { it.getDrawables() }
            .filterIsInstance<TextDrawable>()
            .reversed() // Đảo ngược để kiểm tra các đối tượng trên cùng trước
        
        // Kiểm tra xem người dùng có nhấn vào nút điều khiển của đối tượng đã chọn không
        selectedTextItem?.let { selected ->
            // Kiểm tra nút xóa
            if (selected.containsDeleteControl(event.x, event.y)) {
                onTextDeleteRequestListener?.invoke(selected)
                selectedTextItem = null
                transformMode = TransformMode.NONE
                view.invalidate()
                return
            }
            
            // Kiểm tra nút chỉnh sửa
            if (selected.containsEditControl(event.x, event.y)) {
                onTextEditRequestListener?.invoke(selected)
                transformMode = TransformMode.NONE
                view.invalidate()
                return
            }
            
            // Kiểm tra nút xoay
            if (selected.containsRotateControl(event.x, event.y)) {
                transformMode = TransformMode.ROTATE
                initialItemRotation = selected.rotation
                view.invalidate()
                return
            }
        }
        
        // Kiểm tra xem người dùng có nhấn vào một đối tượng TextDrawable không
        val touchedItem = allTextDrawables.firstOrNull { it.contains(event.x, event.y) }
        
        if (touchedItem != null) {
            selectedTextItem = touchedItem
            initialItemPosition = PointF(touchedItem.position.x, touchedItem.position.y)
            initialItemRotation = touchedItem.rotation
            transformMode = TransformMode.MOVE
            onTextSelectedListener?.invoke(touchedItem)
            view.invalidate()
            return
        } else {
            // Nếu không nhấn vào đối tượng nào, bỏ chọn đối tượng hiện tại
            selectedTextItem = null
            onTextSelectedListener?.invoke(null)
            transformMode = TransformMode.NONE
            view.invalidate()
        }
    }
    
    override fun onTouchMove(event: MotionEvent) {
        val currentPoint = PointF(event.x, event.y)
        val initialPoint = initialTouchPoint ?: return
        
        selectedTextItem?.let { item ->
            when (transformMode) {
                TransformMode.MOVE -> {
                    val dx = currentPoint.x - initialPoint.x
                    val dy = currentPoint.y - initialPoint.y
                    initialItemPosition?.let { initial ->
                        item.position.set(initial.x + dx, initial.y + dy)
                        onTransformationChanged()
                    }
                }
                TransformMode.ROTATE -> {
                    // Tính toán góc xoay dựa trên vị trí của đối tượng và điểm chạm
                    val bounds = item.getBounds()
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    
                    val initialAngle = atan2(
                        initialPoint.y - centerY,
                        initialPoint.x - centerX
                    )
                    val currentAngle = atan2(
                        currentPoint.y - centerY,
                        currentPoint.x - centerX
                    )
                    
                    // Tính toán sự thay đổi góc và cập nhật góc xoay của đối tượng
                    val angleDiff = Math.toDegrees((currentAngle - initialAngle).toDouble()).toFloat()
                    item.rotation = initialItemRotation + angleDiff
                    onTransformationChanged()
                }
                TransformMode.NONE -> {
                    // Không làm gì
                }
            }
        }
    }
    
    override fun onTouchEnd(event: MotionEvent) {
        transformMode = TransformMode.NONE
        initialTouchPoint = null
        initialItemPosition = null
    }
    
    override fun drawPreview(canvas: Canvas) {
        // Vẽ đối tượng đã chọn với các nút điều khiển
        selectedTextItem?.drawWithControls(canvas, 255)
    }
    
    override fun onToolSelected() {
        // Không cần làm gì đặc biệt khi công cụ được chọn
    }
    
    override fun onToolDeselected() {
        selectedTextItem = null
        view.invalidate()
    }
    
    override fun getSelectedItem(): DrawableItem? = selectedTextItem
    
    fun setSelectedItem(item: TextDrawable?) {
        selectedTextItem = item
        onTextSelectedListener?.invoke(item)
        view.invalidate()
    }
    
    override fun onTransformationChanged() {
        if (view is PaintEditorView) {
            view.redrawAllLayers()
        }
        view.invalidate()
    }
    
    private enum class TransformMode {
        NONE,
        MOVE,
        ROTATE
    }
}