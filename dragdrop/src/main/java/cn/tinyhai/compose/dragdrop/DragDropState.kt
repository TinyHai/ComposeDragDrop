package cn.tinyhai.compose.dragdrop

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEachReversed
import cn.tinyhai.compose.dragdrop.helper.AnimatedDragDropHelper
import cn.tinyhai.compose.dragdrop.helper.DragDropHelper
import cn.tinyhai.compose.dragdrop.helper.SimpleDragDropHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

private const val TAG = "DragDropState"

sealed interface DragType {
    data object LongPress : DragType
    data object Immediate : DragType
}

interface DragTargetInfo {

    val isDragging: Boolean
    val isAnimationRunning: Boolean
    val isActive get() = isDragging || isAnimationRunning

    // the dragOffset in DragDropBox
    val dragOffset: Offset
    val dragTargetSnapshot: (DrawScope.() -> Unit)?
    val dragTargetBoundInBox: Rect
    val dataToDrop: DataToDrop<Any?>?

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
    override var isAnimationRunning by mutableStateOf(false)
    override var dragOffset by mutableStateOf(Offset.Zero)
    override var dragTargetBoundInBox by mutableStateOf(Rect.Zero)
    override var dragTargetSnapshot by mutableStateOf<(DrawScope.() -> Unit)?>(null)
    override var dataToDrop by mutableStateOf<DataToDrop<Any?>?>(null)

    open fun reset() {
        isDragging = false
        isAnimationRunning = false
        dragOffset = Offset.Zero
        dragTargetBoundInBox = Rect.Zero
        dragTargetSnapshot = null
        dataToDrop = null
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
        state.registerDropTargetCallback(dropTargetState)
        onDispose {
            state.unregisterDropTarget(dropTargetState)
        }
    }
}

@Composable
fun RegisterDragTarget(dragTargetState: DragTargetState<*>) {
    val state = LocalDragDrop.current
    DisposableEffect(state, dragTargetState) {
        state.registerDragTarget(dragTargetState)
        onDispose {
            state.unregisterDragTarget(dragTargetState)
        }
    }
}

class DragDropState private constructor(
    private val state: DragTargetInfo,
    private val helper: DragDropHelper
) : DragTargetInfo by state {

    private var dragDropBoxCoordinates: LayoutCoordinates? = null

    private val dropTargetCallbacks: MutableList<DropTargetCallback<Any?>> = arrayListOf()
    private val dragTargetCallbacks: MutableList<DragTargetCallback<Any?>> = arrayListOf()

    private var tmpDragTarget: DragTargetCallback<*>? = null

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            return if (isActive || tmpDragTarget != null) available else Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return if (isActive || tmpDragTarget != null) available else Velocity.Zero
        }
    }

    internal fun attach(layoutCoordinates: LayoutCoordinates) {
        this.dragDropBoxCoordinates = layoutCoordinates
    }

    internal fun onDragStart(
        dragStartOffset: Offset
    ) {
        val dragTarget = dragTargetCallbacks.firstOrNull { it.contains(dragStartOffset) }
            ?: throw CancellationException("drag cancel because of no dragTarget contains $dragStartOffset")
        if (tmpDragTarget != null && dragTarget != tmpDragTarget) {
            throw CancellationException("drag cancel because of trying to drag a different dragTarget during dragging")
        }
        tmpDragTarget = dragTarget
        helper.handleDragStart(
            dragTarget.dataToDrop,
            dragStartOffset,
            dragTarget.snapshot,
            dragTarget.boundInBox
        )
        dragTarget.onDragStart()
    }

    internal fun onDrag(dragOffset: Offset) {
        helper.handleDrag(dragOffset)

        dataToDrop?.let { dataToDrop ->
            val lastCallback = lastInBoundDropTarget()
            val dragPosition = helper.currentDragOffset()
            val newCallback = findDropTarget(dragPosition, dataToDrop)
            if (lastCallback !== newCallback) {
                lastCallback?.onDragOut()
                newCallback?.onDragIn(dataToDrop)
            }
        }
    }

    private fun lastInBoundDropTarget(): DropTargetCallback<Any?>? {
        return dropTargetCallbacks.lastOrNull { it.isInBound }
    }

    private fun findDropTarget(
        position: Offset,
        dataToDrop: DataToDrop<*>
    ): DropTargetCallback<Any?>? {
        dropTargetCallbacks.fastForEachReversed {
            if (it.contains(position) && it.isInterest(dataToDrop)) {
                return it
            }
        }
        return null
    }

    internal fun onDragEnd() {
        dataToDrop?.let { dataToDrop ->
            lastInBoundDropTarget()?.let { dropTarget ->
                dropTarget.onDrop(dataToDrop)
                helper.handleDragEnd()
            }
        } ?: return onDragCancel()

        reset()
    }

    internal fun onDragCancel() {
        helper.handleDragCancel(::reset)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun registerDropTargetCallback(dropTargetCallback: DropTargetCallback<*>) {
        dropTargetCallbacks.add(dropTargetCallback as DropTargetCallback<Any?>)
    }

    internal fun unregisterDropTarget(dropTargetCallback: DropTargetCallback<*>) {
        if (dropTargetCallbacks.remove(dropTargetCallback)) {
            dropTargetCallback.onReset()
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun registerDragTarget(dragTargetCallback: DragTargetCallback<*>) {
        dragTargetCallbacks.add(dragTargetCallback as DragTargetCallback<Any?>)
    }

    internal fun unregisterDragTarget(dragTargetCallback: DragTargetCallback<*>) {
        if (dragTargetCallbacks.remove(dragTargetCallback)) {
            dragTargetCallback.onReset()
        }
    }

    fun calculateBoundInBox(
        layoutCoordinates: LayoutCoordinates,
        clipBounds: Boolean = true
    ): Rect {
        return dragDropBoxCoordinates?.localBoundingBoxOf(layoutCoordinates, clipBounds)
            ?: Rect.Zero
    }

    fun currentOverlayOffset() = helper.calculateTargetOffset()

    private fun reset() {
        tmpDragTarget = null
        dragTargetCallbacks.forEach {
            it.onReset()
        }
        dropTargetCallbacks.forEach {
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