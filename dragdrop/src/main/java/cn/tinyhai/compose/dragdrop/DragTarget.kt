package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.tinyhai.compose.dragdrop.modifier.dragTarget

private const val TAG = "DragTarget"

data class DragTargetState<T>(
    val dataToDrop: T?,
    val dragType: DragType,
)

@Composable
fun <T> DragTarget(
    dataToDrop: T?,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    dragType: DragType = LocalDragDrop.current.dragType,
    hiddenOnDragging: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .dragTarget(
                enable,
                dataToDrop,
                dragType,
                content
            )
    ) {
        val state = LocalDragDrop.current
        if (!hiddenOnDragging || !state.isDragging) {
            content()
        }
    }
}