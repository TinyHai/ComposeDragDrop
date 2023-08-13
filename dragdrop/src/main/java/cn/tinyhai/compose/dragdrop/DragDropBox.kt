package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize

private const val TAG = "DragDropBox"

@Composable
fun DragDropBox(
    modifier: Modifier = Modifier,
    scale: Float = 1.2f,
    alpha: Float = 0.9f,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberDragDropState()
    CompositionLocalProvider(
        LocalDragDrop provides state
    ) {
        Box(
            modifier = modifier.onGloballyPositioned {
                state.attach(it)
            },
        ) {
            content()

            if (state.isDragging) {
                val targetSizeDp = with(LocalDensity.current) {
                    state.draggableSizePx.toSize().toDpSize()
                }
                Box(
                    modifier = Modifier
                        .size(targetSizeDp)
                        .graphicsLayer {
                            val offset = state.calculateTargetOffset()
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            translationX = offset.x
                            translationY = offset.y
                        },
                ) {
                    state.draggableComposition?.invoke()
                }
            }
        }
    }
}