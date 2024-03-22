package cn.tinyhai.compose.dragdrop.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import cn.tinyhai.compose.dragdrop.*

fun Modifier.attachAsContainer() = composed {
    val state = LocalDragDrop.current
    this.onGloballyPositioned { state.attach(it) }
}

fun <T> Modifier.dropTarget(
    state: DropTargetState<T>,
    enable: Boolean = true,
) = composed {
    if (enable) {
        RegisterDropTarget(state)

        val dragDropState = LocalDragDrop.current
        onGloballyPositioned {
            dragDropState.boundInBox(it).let { rect ->
                state.boundInBox = rect
            }
        }
    } else {
        Modifier
    }
}

@Composable
fun <T> Modifier.dragTarget(
    dataToDrop: T?,
    draggableComposable: @Composable () -> Unit,
    dragType: DragType = LocalDragDrop.current.dragType,
    enable: Boolean = true
) = composed {
    if (!enable) {
        Modifier
    } else {
        val currentState = LocalDragDrop.current
        val dragTargetState = rememberUpdatedState(newValue = DragTargetState(dataToDrop, dragType))
        var currentOffsetInBox by remember {
            mutableStateOf(Offset.Zero)
        }
        var currentSizePx by remember {
            mutableStateOf(IntSize.Zero)
        }

        this
            .onGloballyPositioned {
                currentOffsetInBox = currentState.positionInBox(it)
                currentSizePx = it.size
            }
            .pointerInput(currentState, dragTargetState, currentSizePx) {
                val onDrag = { _: PointerInputChange, dragAmount: Offset ->
                    currentState.onDrag(dragAmount)
                }
                val onDragStart = { offset: Offset ->
                    currentState.onDragStart(
                        dragTargetState.value.dataToDrop,
                        currentOffsetInBox,
                        offset,
                        draggableComposable,
                        currentSizePx
                    )
                }
                val onDragEnd = {
                    currentState.onDragEnd()
                }
                val onDragCancel = {
                    currentState.onDragCancel()
                }
                when (dragTargetState.value.dragType) {
                    DragType.LongPress -> {
                        detectDragGesturesAfterLongPress(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }

                    DragType.Immediate -> {
                        detectDragGestures(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel
                        )
                    }
                }
            }
    }
}