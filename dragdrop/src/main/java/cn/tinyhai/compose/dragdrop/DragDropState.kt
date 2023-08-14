package cn.tinyhai.compose.dragdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter

private const val TAG = "DragDropState"

sealed interface DragType {
    data object LongPress : DragType
    data object Immediate : DragType
}

interface DragTargetInfo {
    var isDragging: Boolean
    var dragStartPosition: Offset
    var dragOffset: Offset
    var draggableComposition: (@Composable () -> Unit)?
    var draggableSizePx: IntSize
    var dataToDrop: Any?
    var dragType: DragType
    var scaleX: Float
    var scaleY: Float
    var alpha: Float
}

private class DragTargetInfoImpl : DragTargetInfo {
    override var isDragging by mutableStateOf(false)
    override var dragStartPosition by mutableStateOf(Offset.Zero)
    override var dragOffset by mutableStateOf(Offset.Zero)
    override var draggableComposition by mutableStateOf<(@Composable () -> Unit)?>(null)
    override var draggableSizePx by mutableStateOf(IntSize.Zero)
    override var dataToDrop by mutableStateOf<Any?>(null)
    override var dragType by mutableStateOf<DragType>(DragType.LongPress)
    override var scaleX by mutableFloatStateOf(0f)
    override var scaleY by mutableFloatStateOf(0f)
    override var alpha by mutableFloatStateOf(0f)
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

@Suppress("NAME_SHADOWING")
@OptIn(FlowPreview::class)
@Composable
fun <T> RegisterDropTarget(boundInBox: State<Rect?>, onDrop: (T?) -> Unit) {
    val realOnDrop by rememberUpdatedState(onDrop)
    val state = LocalDragDrop.current
    val boundInBoxFlow = remember {
        snapshotFlow {
            boundInBox.value
        }.filter { it != Rect.Zero }.debounce(100)
    }

    val boundInBox = boundInBoxFlow.collectAsState(initial = null)
    boundInBox.value?.let { bound ->
        if (bound != Rect.Zero) {
            DisposableEffect(bound) {
                state.registerDropTarget(bound, realOnDrop)
                onDispose {
                    state.unregisterDropTarget(bound)
                }
            }
        }
    }
}

class DragDropState private constructor(
    private val dragTargetInfo: DragTargetInfo = DragTargetInfoImpl(),
) : DragTargetInfo by dragTargetInfo {

    private var dragDropBoxCoordinates: LayoutCoordinates? = null

    private val dropTargets = hashMapOf<Rect, (Any?) -> Unit>()

    fun attach(layoutCoordinates: LayoutCoordinates) {
        this.dragDropBoxCoordinates = layoutCoordinates
    }

    fun <T> onDrop(offset: Offset, dataToDrop: T?) {
        val onDrop =
            dropTargets.firstNotNullOfOrNull { if (it.key.contains(offset)) it.value else null }
        onDrop?.invoke(dataToDrop)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> registerDropTarget(bound: Rect, onDrop: (T?) -> Unit) {
        dropTargets[bound] = onDrop as (Any?) -> Unit
    }

    fun unregisterDropTarget(bound: Rect) {
        dropTargets.remove(bound)
    }

    fun offsetInBox(dragTargetLayoutCoordinates: LayoutCoordinates): Offset {
        return dragDropBoxCoordinates!!.localPositionOf(dragTargetLayoutCoordinates, Offset.Zero)
    }

    fun boundInBox(dropTargetLayoutCoordinates: LayoutCoordinates): Rect {
        return dragDropBoxCoordinates!!.localBoundingBoxOf(dropTargetLayoutCoordinates)
    }

    fun calculateTargetOffset() = dragStartPosition + dragOffset - draggableSizePx.center.toOffset()

    fun calculateDragPosition() = dragStartPosition + dragOffset

    companion object {
        operator fun invoke(
            scaleX: Float,
            scaleY: Float,
            alpha: Float,
            dragType: DragType
        ): DragDropState {
            return DragDropState().apply {
                this.scaleX = scaleX
                this.scaleY = scaleY
                this.alpha = alpha
                this.dragType = dragType
            }
        }
    }
}