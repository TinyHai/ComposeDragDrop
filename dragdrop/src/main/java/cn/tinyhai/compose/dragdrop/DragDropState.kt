package cn.tinyhai.compose.dragdrop

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset

private const val TAG = "DragDropState"

sealed interface DragType {
    data object LongPress : DragType
    data object Immediate : DragType
}

interface DragTargetInfo {
    val isDragging: Boolean
    val dragStartPosition: Offset
    val dragOffset: Offset
    val draggableComposition: (@Composable () -> Unit)?
    val draggableSizePx: IntSize
    val dataToDrop: Any?

    val scaleX: Float
    val scaleY: Float
    val alpha: Float
    val dragType: DragType
}

internal interface DragDropCallback<T> {
    val isInBound: Boolean

    fun onDrag(dragPosition: Offset): Boolean

    fun onDragIn(dataToDrop: T)

    fun onDragOut()

    fun onDrop(dataToDrop: T)

    fun onReset()
}

private class DragTargetInfoImpl(
    override val scaleX: Float,
    override val scaleY: Float,
    override val alpha: Float,
    override val dragType: DragType,
) : DragTargetInfo {
    override var isDragging by mutableStateOf(false)
    override var dragStartPosition by mutableStateOf(Offset.Zero)
    override var dragOffset by mutableStateOf(Offset.Zero)
    override var draggableComposition by mutableStateOf<(@Composable () -> Unit)?>(null)
    override var draggableSizePx by mutableStateOf(IntSize.Zero)
    override var dataToDrop by mutableStateOf<Any?>(null)
}

@Composable
fun rememberDragDropState(
    scaleX: Float,
    scaleY: Float,
    alpha: Float = 0.9f,
    defaultDragType: DragType = DragType.LongPress
): DragDropState {
    return remember(scaleX, scaleY, alpha, defaultDragType) {
        DragDropState(scaleX, scaleY, alpha, defaultDragType)
    }
}

@Composable
fun rememberDragDropState(
    scale: Float = 1.2f,
    alpha: Float = 0.9f,
    dragType: DragType = DragType.LongPress
): DragDropState {
    return rememberDragDropState(scale, scale, alpha, dragType)
}

@Composable
fun RegisterDropTarget(dropTargetState: DropTargetState<*>) {
    val state = LocalDragDrop.current
    DisposableEffect(state, dropTargetState) {
        val callback = dropTargetState.dragDropCallback
        state.registerDragDropCallback(callback)
        onDispose {
            state.unregisterDropTarget(callback)
        }
    }
}

class DragDropState private constructor(
    private val dragTargetInfo: DragTargetInfoImpl,
) : DragTargetInfo by dragTargetInfo {
    override var isDragging: Boolean by dragTargetInfo::isDragging
        private set
    override var dragStartPosition: Offset by dragTargetInfo::dragStartPosition
        private set
    override var dragOffset: Offset by dragTargetInfo::dragOffset
        private set
    override var draggableComposition: @Composable (() -> Unit)? by dragTargetInfo::draggableComposition
        private set
    override var draggableSizePx: IntSize by dragTargetInfo::draggableSizePx
        private set
    override var dataToDrop: Any? by dragTargetInfo::dataToDrop
        private set

    private var dragDropBoxCoordinates: LayoutCoordinates? = null

    private val callbacks: MutableList<DragDropCallback<Any?>> = arrayListOf()

    private fun reset() {
        isDragging = false
        dragStartPosition = Offset.Zero
        dragOffset = Offset.Zero
        draggableComposition = null
        draggableSizePx = IntSize.Zero
        dataToDrop = null
        callbacks.forEach {
            it.onReset()
        }
    }

    internal fun attach(layoutCoordinates: LayoutCoordinates) {
        this.dragDropBoxCoordinates = layoutCoordinates
    }

    internal fun onDragStart(
        dataToDrop: Any?,
        offsetInBox: Offset,
        dragStartOffset: Offset,
        content: @Composable () -> Unit,
        contentSizePx: IntSize
    ) {
        isDragging = true
        this.dataToDrop = dataToDrop
        dragStartPosition = offsetInBox + dragStartOffset
        draggableComposition = content
        draggableSizePx = contentSizePx
    }

    internal fun onDrag(dragAmount: Offset) {
        dragOffset += dragAmount

        val lastCallback = callbacks.lastOrNull { it.isInBound }
        val dragPosition = calculateDragPosition()
        val newCallback = callbacks.lastOrNull { it.onDrag(dragPosition) }
        if (lastCallback !== newCallback) {
            lastCallback?.onDragOut()
            newCallback?.onDragIn(dataToDrop)
        }
    }

    internal fun onDragEnd() {
        callbacks.lastOrNull { it.isInBound }?.onDrop(dataToDrop)
        reset()
    }

    internal fun onDragCancel() {
        reset()
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> registerDragDropCallback(dragDropCallback: DragDropCallback<T>) {
        callbacks.add(dragDropCallback as DragDropCallback<Any?>)
    }

    internal fun unregisterDropTarget(dragDropCallback: DragDropCallback<*>) {
        callbacks.remove(dragDropCallback)
    }

    internal fun positionInBox(dragTargetLayoutCoordinates: LayoutCoordinates): Offset {
        return dragDropBoxCoordinates!!.localPositionOf(dragTargetLayoutCoordinates, Offset.Zero)
    }

    internal fun boundInBox(dropTargetLayoutCoordinates: LayoutCoordinates): Rect {
        return dragDropBoxCoordinates!!.localBoundingBoxOf(dropTargetLayoutCoordinates)
    }

    fun calculateTargetOffset() = dragStartPosition + dragOffset - draggableSizePx.center.toOffset()

    private fun calculateDragPosition() = dragStartPosition + dragOffset

    companion object {
        operator fun invoke(
            scaleX: Float,
            scaleY: Float,
            alpha: Float,
            defaultDragType: DragType
        ): DragDropState {
            return DragDropState(DragTargetInfoImpl(scaleX, scaleY, alpha, defaultDragType))
        }
    }
}