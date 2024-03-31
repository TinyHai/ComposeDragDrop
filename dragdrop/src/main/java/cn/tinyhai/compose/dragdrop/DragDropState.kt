package cn.tinyhai.compose.dragdrop

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import cn.tinyhai.compose.dragdrop.helper.AnimatedDragDropHelper
import cn.tinyhai.compose.dragdrop.helper.DragDropHelper
import cn.tinyhai.compose.dragdrop.helper.SimpleDragDropHelper
import kotlinx.coroutines.CoroutineScope

private const val TAG = "DragDropState"

sealed interface DragType {
    data object LongPress : DragType
    data object Immediate : DragType
}

interface DragTargetInfo {
    val isDragging: Boolean
    val dragOffsetInDragTarget: Offset
    val dragTargetContent: (@Composable () -> Unit)?
    val dragTargetOffsetInBox: Offset
    val dragTargetContentSizePx: IntSize
    val dataToDrop: Any?
    val targetKey: Any?

    val scaleX: Float
    val scaleY: Float
    val alpha: Float
    val dragType: DragType
}

internal open class SimpleDragTargetInfo(
    override val scaleX: Float,
    override val scaleY: Float,
    override val alpha: Float,
    override val dragType: DragType,
) : DragTargetInfo {
    override var isDragging by mutableStateOf(false)
    override var dragOffsetInDragTarget by mutableStateOf(Offset.Zero)
    override var dragTargetContent by mutableStateOf<(@Composable () -> Unit)?>(null)
    override var dragTargetOffsetInBox by mutableStateOf(Offset.Zero)
    override var dragTargetContentSizePx by mutableStateOf(IntSize.Zero)
    override var dataToDrop by mutableStateOf<Any?>(null)
    override var targetKey by mutableStateOf<Any?>(null)

    open fun reset() {
        isDragging = false
        dragOffsetInDragTarget = Offset.Zero
        dragTargetContent = null
        dragTargetOffsetInBox = Offset.Zero
        dragTargetContentSizePx = IntSize.Zero
        dataToDrop = null
        targetKey = null
    }
}

internal class AnimatedDragTargetInfo(
    private val scaleXDelta: Float,
    private val scaleYDelta: Float,
    private val alphaDelta: Float,
    dragType: DragType,
) : SimpleDragTargetInfo(1f, 1f, 1f, dragType) {
    var animatableValue by mutableFloatStateOf(0f)
    override val scaleX get() = animatableValue * scaleXDelta + super.scaleX
    override val scaleY get() = animatableValue * scaleYDelta + super.scaleY
    override val alpha get() = animatableValue * alphaDelta + super.alpha
    override fun reset() {
        super.reset()
        animatableValue = 0f
    }
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
fun rememberAnimatedDragDropState(
    scaleX: Float,
    scaleY: Float,
    alpha: Float = 0.9f,
    startSpec: AnimationSpec<Float> = tween(),
    endSpec: AnimationSpec<Float> = tween(400),
    defaultDragType: DragType = DragType.LongPress
): DragDropState {
    val scope = rememberCoroutineScope()
    return remember(scaleX, scaleY, alpha, defaultDragType, startSpec, endSpec, scope) {
        DragDropState(scaleX, scaleY, alpha, defaultDragType, startSpec, endSpec, scope)
    }
}

@Composable
fun rememberDragDropState(
    scale: Float = 1.2f,
    alpha: Float = 0.9f,
    enableAnimation: Boolean = false,
    dragType: DragType = DragType.LongPress
): DragDropState {
    return if (enableAnimation) {
        rememberAnimatedDragDropState(scale, scale, alpha, defaultDragType = dragType)
    } else {
        rememberDragDropState(scale, scale, alpha, dragType)
    }
}

@Composable
fun RegisterDropTarget(dropTargetState: DropTargetState<*>) {
    val state = LocalDragDrop.current
    DisposableEffect(state, dropTargetState) {
        state.registerDragDropCallback(dropTargetState)
        onDispose {
            state.unregisterDropTarget(dropTargetState)
        }
    }
}

class DragDropState private constructor(
    private val state: DragTargetInfo,
    private val helper: DragDropHelper
) : DragTargetInfo by state {

    private var dragDropBoxCoordinates: LayoutCoordinates? = null

    private val callbacks: MutableList<DropTargetCallback<Any?>> = arrayListOf()

    internal fun attach(layoutCoordinates: LayoutCoordinates) {
        this.dragDropBoxCoordinates = layoutCoordinates
    }

    internal fun onDragStart(
        dragTargetKey: Any?,
        dataToDrop: Any?,
        offsetInBox: Offset,
        dragStartOffset: Offset,
        content: @Composable () -> Unit,
        contentSizePx: IntSize
    ) {
        helper.handleDragStart(
            dragTargetKey,
            dataToDrop,
            offsetInBox,
            dragStartOffset,
            content,
            contentSizePx
        )
    }

    internal fun onDrag(dragOffset: Offset) {
        helper.handleDrag(dragOffset)

        val lastCallback = callbacks.lastOrNull { it.isInBound }
        val dragPosition = helper.calculateDragPosition()
        val newCallback = callbacks.lastOrNull { it.contains(dragPosition) }
        if (lastCallback !== newCallback) {
            lastCallback?.onDragOut()
            newCallback?.onDragIn(dataToDrop)
        }
    }

    internal fun onDragEnd() {
        callbacks.lastOrNull { it.isInBound }?.let {
            it.onDrop(dataToDrop)
            helper.handleDragEnd()
        } ?: helper.handleDragCancel()
        reset()
    }

    internal fun onDragCancel() {
        helper.handleDragCancel()
        reset()
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> registerDragDropCallback(dragDropCallback: DropTargetCallback<T>) {
        callbacks.add(dragDropCallback as DropTargetCallback<Any?>)
    }

    internal fun unregisterDropTarget(dragDropCallback: DropTargetCallback<*>) {
        callbacks.remove(dragDropCallback)
    }

    fun positionInBox(dragTargetLayoutCoordinates: LayoutCoordinates): Offset {
        return dragDropBoxCoordinates?.localPositionOf(dragTargetLayoutCoordinates, Offset.Zero)
            ?: Offset.Unspecified
    }

    fun calculateBoundInBox(dropTargetLayoutCoordinates: LayoutCoordinates): Rect {
        return dragDropBoxCoordinates?.localBoundingBoxOf(dropTargetLayoutCoordinates) ?: Rect.Zero
    }

    fun currentOverlayOffset() = helper.calculateTargetOffset()

    private fun reset() {
        callbacks.forEach {
            it.onReset()
        }
    }

    companion object {
        operator fun invoke(
            scaleX: Float,
            scaleY: Float,
            alpha: Float,
            defaultDragType: DragType,
        ): DragDropState {
            val state = SimpleDragTargetInfo(scaleX, scaleY, alpha, defaultDragType)
            val helper = SimpleDragDropHelper(state)
            return DragDropState(state, helper)
        }

        operator fun invoke(
            scaleX: Float,
            scaleY: Float,
            alpha: Float,
            defaultDragType: DragType,
            startSpec: AnimationSpec<Float>,
            endSpec: AnimationSpec<Float>,
            scope: CoroutineScope
        ): DragDropState {
            val state =
                AnimatedDragTargetInfo(scaleX - 1f, scaleY - 1f, alpha - 1f, defaultDragType)
            val helper = AnimatedDragDropHelper(state, startSpec, endSpec, scope)
            return DragDropState(state, helper)
        }
    }
}