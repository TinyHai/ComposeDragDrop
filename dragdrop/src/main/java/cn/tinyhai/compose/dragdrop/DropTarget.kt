package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import cn.tinyhai.compose.dragdrop.modifier.dropTarget

private const val TAG = "DropTarget"

interface DropTargetCallback<T> {
    val isInBound: Boolean

    fun contains(dragPosition: Offset): Boolean

    fun onDragIn(dataToDrop: T)

    fun onDragOut()

    fun onDrop(dataToDrop: T)

    fun onReset()
}

class DropTargetState<T>(
    private val onDrop: ((T?) -> Unit)?,
) : DropTargetCallback<T> {
    var boundInBox: Rect = Rect.Zero
    override var isInBound by mutableStateOf(false)
        private set
    var dataToDrop by mutableStateOf<T?>(null)
        private set

    override fun contains(dragPosition: Offset): Boolean {
        return boundInBox.contains(dragPosition)
    }

    override fun onDragOut() {
        isInBound = false
        this.dataToDrop = null
    }

    override fun onReset() {
        isInBound = false
        dataToDrop = null
    }

    override fun onDrop(dataToDrop: T) {
        if (dataToDrop == this.dataToDrop) {
            onDrop?.invoke(dataToDrop)
        }
    }

    override fun onDragIn(dataToDrop: T) {
        isInBound = true
        this.dataToDrop = dataToDrop
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