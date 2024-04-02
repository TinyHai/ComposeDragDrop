package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import cn.tinyhai.compose.dragdrop.modifier.dragTarget

private const val TAG = "DragTarget"

interface DragTargetCallback<T> {
    val dataToDrop: T?
    val boundInBox: Rect

    val content: @Composable () -> Unit
    fun contains(position: Offset): Boolean

    fun onDragStart()

    fun onDragEnd()

    fun onReset()
}

class DragTargetState<T>(
    override var dataToDrop: T?,
    override val content: @Composable () -> Unit,
) : DragTargetCallback<T> {

    var isDragging: Boolean by mutableStateOf(false)

    override var boundInBox: Rect = Rect.Zero

    override fun onDragStart() {
        isDragging = true
    }

    override fun onDragEnd() {
        isDragging = false
    }

    override fun onReset() {
        isDragging = false
    }

    override fun contains(position: Offset): Boolean {
        return boundInBox.contains(position)
    }

    override fun toString(): String {
        return "DragTargetState(dataToDrop: $dataToDrop, isDragging: $isDragging, boundInBox: $boundInBox)"
    }
}

@Composable
fun <T> rememberDragTargetState(
    dataToDrop: T?,
    draggableContent: @Composable () -> Unit
): DragTargetState<T> {
    return remember(dataToDrop, draggableContent) {
        DragTargetState(dataToDrop, draggableContent)
    }
}

@Composable
fun <T> DragTarget(
    dataToDrop: T?,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    hiddenOnDragging: Boolean = false,
    content: @Composable () -> Unit
) {
    val state = rememberDragTargetState(dataToDrop, content)
    Box(
        modifier = modifier
            .dragTarget(
                enable = enable,
                state = state
            )
    ) {
        val dragDropState = LocalDragDrop.current
        when {
            hiddenOnDragging && dragDropState.isDragging -> {
                if (!state.isDragging) {
                    content()
                }
            }

            else -> content()
        }
    }
}