package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

private const val TAG = "DragTarget"

@Composable
fun <T> DragTarget(
    dataToDrop: T?,
    modifier: Modifier = Modifier,
    dragType: DragType = LocalDragDrop.current.dragType,
    content: @Composable () -> Unit
) {
    val currentState = LocalDragDrop.current
    var currentOffsetInBox by remember {
        mutableStateOf(Offset.Zero)
    }
    var currentSizePx by remember {
        mutableStateOf(IntSize.Zero)
    }
    Box(
        modifier = modifier
            .onGloballyPositioned {
                currentOffsetInBox = currentState.offsetInBox(it)
                currentSizePx = it.size
            }
            .pointerInput(currentState, currentOffsetInBox, currentSizePx, dragType) {
                val onDrag = { change: PointerInputChange, dragAmount: Offset ->
                    change.consume()
                    currentState.dragOffset += dragAmount
                }
                val onDragStart = { offset: Offset ->
                    currentState.apply {
                        isDragging = true
                        this.dataToDrop = dataToDrop
                        dragStartPosition = currentOffsetInBox + offset
                        draggableComposition = content
                        draggableSizePx = currentSizePx
                    }
                    Unit
                }
                val onDragEnd = {
                    currentState.onDrop(
                        currentState.dragStartPosition + currentState.dragOffset,
                        currentState.dataToDrop
                    )
                    currentState.apply {
                        isDragging = false
                        dragOffset = Offset.Zero
                    }
                    Unit
                }
                when (dragType) {
                    DragType.LongPress -> {
                        detectDragGesturesAfterLongPress(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                        )
                    }

                    DragType.Immediate -> {
                        detectDragGestures(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd
                        )
                    }
                }
            },
    ) {
        content()
    }
}