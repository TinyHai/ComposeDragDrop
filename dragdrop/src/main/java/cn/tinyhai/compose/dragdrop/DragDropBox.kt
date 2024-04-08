package cn.tinyhai.compose.dragdrop

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import cn.tinyhai.compose.dragdrop.modifier.attachAsContainer

private const val TAG = "DragDropBox"

@Composable
fun DragDropBox(
    modifier: Modifier = Modifier,
    scale: Float = 1.2f,
    alpha: Float = 0.9f,
    defaultDragType: DragType = DragType.LongPress,
    state: DragDropState = rememberDragDropState(scale, scale, alpha, defaultDragType),
    content: @Composable BoxScope.() -> Unit
) {
    DragDropBox(state, modifier, content)
}

@Composable
fun AnimatedDragDropBox(
    modifier: Modifier = Modifier,
    scale: Float = 1.2f,
    alpha: Float = 0.9f,
    startSpec: AnimationSpec<Float> = tween(),
    endSpec: AnimationSpec<Float> = tween(400),
    defaultDragType: DragType = DragType.LongPress,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberAnimatedDragDropState(
        scale,
        scale,
        alpha,
        startSpec,
        endSpec,
        defaultDragType
    )
    DragDropBox(state, modifier, content)
}

@Composable
fun DragDropBox(
    state: DragDropState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalDragDrop provides state
    ) {
        Box(
            modifier = modifier.attachAsContainer(),
        ) {
            content()
            DragDropOverlay()
        }
    }
}

@Composable
fun DragDropOverlay(state: DragDropState = LocalDragDrop.current) {
    if (state.isActive) {
        val size = with(LocalDensity.current) {
            state.dragTargetBoundInBox.size.toDpSize()
        }
        Canvas(
            modifier = Modifier
                .size(size)
                .absoluteOffset {
                    state
                        .currentOverlayOffset()
                        .round()
                }
                .graphicsLayer {
                    scaleX = state.scaleX
                    scaleY = state.scaleY
                    alpha = state.alpha
                }
        ) {
            state.dragTargetSnapshot?.invoke(this)
        }
    }
}