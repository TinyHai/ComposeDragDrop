package cn.tinyhai.compose.dragdrop.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import cn.tinyhai.compose.dragdrop.*
import kotlinx.coroutines.flow.collectLatest

@Suppress("UNCHECKED_CAST")
fun <T> Modifier.dropTarget(
    state: DropTargetState<T>, enabled: Boolean = true, onDrop: (T?) -> Unit
) = composed {
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

fun <T> Modifier.dragTarget(
    enable: Boolean, dataToDrop: T?, dragType: DragType, draggableComposable: @Composable () -> Unit
) = composed {
    val currentState = LocalDragDrop.current
    val dragTargetState = rememberUpdatedState(newValue = DragTargetState(dataToDrop, dragType))
    var currentOffsetInBox by remember {
        mutableStateOf(Offset.Zero)
    }
    var currentSizePx by remember {
        mutableStateOf(IntSize.Zero)
    }
    if (!enable) {
        Modifier
    } else {
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