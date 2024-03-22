package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import cn.tinyhai.compose.dragdrop.modifier.dropTarget

private const val TAG = "DropTarget"

class DropTargetState<T>(
    private val onDrop: ((T?) -> Unit)?,
) {
    var boundInBox: Rect = Rect.Zero
    var isInBound by mutableStateOf(false)
        private set
    var dataToDrop by mutableStateOf<T?>(null)
        private set

    internal val dragDropCallback = object : DragDropCallback<T> {
        override val isInBound: Boolean
            get() = this@DropTargetState.isInBound

        override fun onDrag(dragPosition: Offset): Boolean {
            return boundInBox.contains(dragPosition)
        }

        override fun onDragOut() {
            this@DropTargetState.isInBound = false
            dataToDrop = null
        }

        override fun onDragIn(dataToDrop: T) {
            this@DropTargetState.isInBound = true
            this@DropTargetState.dataToDrop = dataToDrop
        }

        override fun onDrop(dataToDrop: T) {
            if (isInBound) {
                onDrop?.invoke(dataToDrop)
            }
        }

        override fun onReset() {
            this@DropTargetState.isInBound = false
            dataToDrop = null
        }
    }

    operator fun component1() = isInBound

    operator fun component2() = dataToDrop

    override fun toString(): String {
        return "DropTargetState(isInBound=$isInBound, dataToDrop=$dataToDrop)"
    }
}

@Composable
fun <T> rememberDropTargetState(onDrop: (T?) -> Unit): DropTargetState<T> {
    val curOnDrop by rememberUpdatedState(newValue = onDrop)
    return remember {
        DropTargetState { curOnDrop(it) }
    }
}

@Composable
fun <T> DropTarget(
    onDrop: (T?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dropTargetState: DropTargetState<T> = rememberDropTargetState(onDrop),
    content: @Composable BoxScope.(isInBound: Boolean, data: T?) -> Unit
) {
    Box(modifier = modifier.dropTarget(dropTargetState, enabled)) {
        content(dropTargetState.isInBound, dropTargetState.dataToDrop)
    }
}