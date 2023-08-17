package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.tinyhai.compose.dragdrop.modifier.dropTarget

private const val TAG = "DropTarget"

class DropTargetState<T> {
    var isInBound by mutableStateOf(false)
        internal set
    var dataToDrop by mutableStateOf<T?>(null)
        internal set

    operator fun component1() = isInBound

    operator fun component2() = dataToDrop

    override fun toString(): String {
        return "DropTargetState(isInBound=$isInBound, dataToDrop=$dataToDrop)"
    }
}

@Composable
fun <T> rememberDropTargetState() = remember {
    DropTargetState<T>()
}

@Composable
fun <T> DropTarget(
    onDrop: (T?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dropTargetState: DropTargetState<T> = rememberDropTargetState(),
    content: @Composable BoxScope.(isInBound: Boolean, data: T?) -> Unit
) {
    Box(modifier = modifier.dropTarget(dropTargetState, enabled, onDrop)) {
        content(dropTargetState.isInBound, dropTargetState.dataToDrop)
    }
}