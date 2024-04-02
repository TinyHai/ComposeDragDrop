package cn.tinyhai.compose.dragdrop.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import cn.tinyhai.compose.dragdrop.*

private const val TAG = "Modifiers"

fun Modifier.attachAsContainer() = composed {
    val state = LocalDragDrop.current
    this
        .onGloballyPositioned { state.attach(it) }
        .nestedScroll(state.nestedScrollConnection)
        .pointerInput(state) {
            when (state.dragType) {
                DragType.LongPress -> {
                    detectDragGesturesAfterLongPress(
                        onDragStart = state::onDragStart,
                        onDrag = { change, _ -> state.onDrag(change.position) },
                        onDragEnd = state::onDragEnd,
                        onDragCancel = state::onDragCancel
                    )
                }

                DragType.Immediate -> {
                    detectDragGestures(
                        onDragStart = state::onDragStart,
                        onDrag = { change, _ -> state.onDrag(change.position) },
                        onDragEnd = state::onDragEnd,
                        onDragCancel = state::onDragCancel
                    )
                }
            }
        }
}

fun <T> Modifier.dropTarget(
    state: DropTargetState<T>,
    enable: Boolean = true,
) = composed {
    if (enable) {
        val dragDropState = LocalDragDrop.current
        RegisterDropTarget(state)

        this
            .onGloballyPositioned {
                state.boundInBox = dragDropState.calculateBoundInBox(it)
            }
    } else {
        Modifier
    }
}

@Composable
fun <T> Modifier.dragTarget(
    state: DragTargetState<T>,
    enable: Boolean = true,
) = composed {
    if (!enable) {
        Modifier
    } else {
        val dragDropState = LocalDragDrop.current
        RegisterDragTarget(dragTargetState = state)

        this
            .onGloballyPositioned {
                state.boundInBox = dragDropState.calculateBoundInBox(it, clipBounds = false)
            }
    }
}