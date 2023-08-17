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
                val onDrag = { _: PointerInputChange, dragAmount: Offset ->
                    currentState.onDrag(dragAmount)
                }
                val onDragStart = { offset: Offset ->
                    currentState.onDragStart(
                        dataToDrop,
                        currentOffsetInBox,
                        offset,
                        content,
                        currentSizePx
                    )
                }
                val onDragEnd = {
                    currentState.onDragEnd()
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