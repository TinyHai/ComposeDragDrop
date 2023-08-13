package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned

private const val TAG = "DropTarget"

@Composable
fun <T> DropTarget(
    onDrop: (T?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isInBound: Boolean, data: T?) -> Unit
) {
    val boundInBox = remember {
        mutableStateOf<Rect?>(null)
    }
    RegisterDropTarget(boundInBox, onDrop)

    val dragDropState = LocalDragDrop.current
    val isInBound by remember {
        derivedStateOf {
            boundInBox.value?.contains(dragDropState.calculateDragPosition()) ?: false
        }
    }
    val data by remember {
        derivedStateOf {
            if (isInBound) {
                dragDropState.dataToDrop as T?
            } else {
                null
            }
        }
    }
    Box(
        modifier = modifier.onGloballyPositioned {
            dragDropState.boundInBox(it).let { rect ->
                boundInBox.value = rect
            }
        },
    ) {
        content(isInBound, data)
    }
}