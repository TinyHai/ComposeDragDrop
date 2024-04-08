package cn.tinyhai.compose.dragdrop.modifier

import android.graphics.Picture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
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
            .then(
                DragTargetDrawModifierElement {
                    state.picture = it
                }
            )
            .onGloballyPositioned {
                state.boundInBox = dragDropState.calculateBoundInBox(it, clipBounds = false)
            }
    }
}

private data class DragTargetDrawModifierElement(
    private val onPictureCreate: (Picture) -> Unit
) : ModifierNodeElement<DragTargetDrawModifier>() {
    override fun create(): DragTargetDrawModifier {
        return DragTargetDrawModifier(onPictureCreate)
    }

    override fun update(node: DragTargetDrawModifier) {
        node.onPictureCreate = this.onPictureCreate
    }

    override fun InspectorInfo.inspectableProperties() {
        properties["onPictureCreate"] = onPictureCreate
    }
}

private class DragTargetDrawModifier(
    onPictureCreate: (Picture) -> Unit
) : Modifier.Node(), DrawModifierNode {

    var onPictureCreate: (Picture) -> Unit = onPictureCreate
        set(value) {
            field = value
            picture = null
            invalidateDraw()
        }

    private var picture: Picture? = null

    private fun getOrCreatePicture(): Picture {
        return picture ?: Picture().also { picture = it }.also { onPictureCreate(it) }
    }

    override fun ContentDrawScope.draw() {
        val picture = getOrCreatePicture()
        val drawScope = this
        val pictureCanvas = Canvas(picture.beginRecording(size.width.toInt(), size.height.toInt()))
        draw(drawScope, layoutDirection, pictureCanvas, size) {
            drawScope.drawContent()
        }
        picture.endRecording()
        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
    }
}