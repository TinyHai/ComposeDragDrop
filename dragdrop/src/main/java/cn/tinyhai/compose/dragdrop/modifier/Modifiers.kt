package cn.tinyhai.compose.dragdrop.modifier

import android.graphics.Picture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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

@Composable
inline fun <reified T> Modifier.dropTarget(
    noinline onDrop: (T?) -> Unit,
    enable: Boolean
): Modifier {
    return dropTarget(state = rememberDropTargetState(onDrop), enable)
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
        SideEffect {
            state.onReset()
        }
        Modifier
    }
}

@Composable
inline fun <reified T> Modifier.dragTarget(
    dataToDrop: T?,
    enable: Boolean = true,
    hiddenWhileDragging: Boolean = false
): Modifier {
    return dragTarget(
        state = rememberDragTargetState(dataToDrop),
        enable = enable,
        hiddenWhileDragging = hiddenWhileDragging
    )
}

@Composable
inline fun <reified T> Modifier.dragTarget(
    noinline dataProvider: () -> T?,
    enable: Boolean = true,
    hiddenWhileDragging: Boolean = false
): Modifier {
    return dragTarget(
        state = rememberDragTargetState(dataProvider),
        enable = enable,
        hiddenWhileDragging = hiddenWhileDragging
    )
}

fun <T> Modifier.dragTarget(
    state: DragTargetState<T>,
    enable: Boolean = true,
    hiddenWhileDragging: Boolean = false
) = composed {
    if (!enable) {
        Modifier
    } else {
        val dragDropState = LocalDragDrop.current
        RegisterDragTarget(dragTargetState = state)

        val alphaModifier =
            if (hiddenWhileDragging && state.isDragging) Modifier.alpha(0f) else Modifier

        this
            .then(alphaModifier)
            .drawWithCache {
                val picture = Picture()
                onDrawWithContent {
                    val canvas =
                        Canvas(picture.beginRecording(size.width.toInt(), size.height.toInt()))
                    draw(this, layoutDirection, canvas, size) {
                        this@onDrawWithContent.drawContent()
                    }
                    picture.endRecording()
                    state.picture = picture
                    drawIntoCanvas { it.nativeCanvas.drawPicture(picture) }
                }
            }
            .onGloballyPositioned {
                state.boundInBox = dragDropState.calculateBoundInBox(it, clipBounds = false)
            }
    }
}