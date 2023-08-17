package cn.tinyhai.compose.dragdrop.modifier

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import cn.tinyhai.compose.dragdrop.DropTargetState
import cn.tinyhai.compose.dragdrop.LocalDragDrop
import cn.tinyhai.compose.dragdrop.RegisterDropTarget
import kotlinx.coroutines.flow.collectLatest

@Suppress("UNCHECKED_CAST")
fun <T> Modifier.dropTarget(
    state: DropTargetState<T>,
    enabled: Boolean = true,
    onDrop: (T?) -> Unit
) =
    composed {
        val isEnabled by rememberUpdatedState(newValue = enabled)
        val boundInBox = remember {
            mutableStateOf<Rect?>(null)
        }

        if (isEnabled) {
            RegisterDropTarget(boundInBox, onDrop)
        }

        val dragDropState = LocalDragDrop.current
        LaunchedEffect(isEnabled) {
            if (isEnabled) {
                snapshotFlow {
                    val isInBound =
                        boundInBox.value?.contains(dragDropState.calculateDragPosition()) ?: false
                    isInBound to (if (isInBound) dragDropState.dataToDrop as? T else null)
                }.collectLatest { (isInBound, dataToDrop) ->
                    state.apply {
                        this.isInBound = isInBound
                        this.dataToDrop = dataToDrop
                    }
                }
            } else {
                state.apply {
                    isInBound = false
                    dataToDrop = null
                }
            }
        }
        onGloballyPositioned {
            dragDropState.boundInBox(it).let { rect ->
                boundInBox.value = rect
            }
        }
    }