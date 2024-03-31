package cn.tinyhai.compose.dragdrop.helper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import cn.tinyhai.compose.dragdrop.AnimatedDragTargetInfo
import cn.tinyhai.compose.dragdrop.SimpleDragTargetInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "DragDropHelper"

interface DragDropHelper {
    fun handleDragStart(
        dragTargetKey: Any?,
        dataToDrop: Any?,
        offsetInBox: Offset,
        dragStartOffset: Offset,
        content: @Composable () -> Unit,
        contentSizePx: IntSize
    )
    fun handleDrag(dragOffset: Offset)
    fun handleDragEnd()
    fun handleDragCancel()
    fun calculateTargetOffset(): Offset
    fun calculateDragPosition(): Offset
}

internal open class SimpleDragDropHelper(
    private val state: SimpleDragTargetInfo
) : DragDropHelper {

    override fun handleDragStart(
        dragTargetKey: Any?,
        dataToDrop: Any?,
        offsetInBox: Offset,
        dragStartOffset: Offset,
        content: @Composable () -> Unit,
        contentSizePx: IntSize
    ) {
        state.apply {
            isDragging = true
            this.dataToDrop = dataToDrop
            targetKey = dragTargetKey
            dragTargetContent = content
            dragTargetOffsetInBox = offsetInBox
            dragTargetContentSizePx = contentSizePx
            dragOffsetInDragTarget = dragStartOffset
        }
    }

    override fun handleDrag(dragOffset: Offset) {
        state.dragOffsetInDragTarget = dragOffset
    }

    override fun handleDragEnd() {
        reset()
    }

    override fun handleDragCancel() {
        reset()
    }

    override fun calculateTargetOffset(): Offset {
        return calculateDragPosition() - state.dragTargetContentSizePx.center.toOffset()
    }

    override fun calculateDragPosition(): Offset {
        return state.dragTargetOffsetInBox + state.dragOffsetInDragTarget
    }

    private fun reset() {
        state.reset()
    }
}

internal class AnimatedDragDropHelper(
    private val state: AnimatedDragTargetInfo,
    private val startSpec: AnimationSpec<Float>,
    private val endSpec: AnimationSpec<Float>,
    private val scope: CoroutineScope
) : SimpleDragDropHelper(state) {

    private var animatable: Animatable<Float, AnimationVector1D> = Animatable(0f)

    init {
        scope.launch {
            snapshotFlow { animatable.value }.collectLatest { state.animatableValue = it }
        }
    }

    private fun startDragStartAnimation() {
        scope.launch {
            animatable.apply {
                animateTo(1f, startSpec)
            }
        }
    }

    private fun startDragCancelAnimation(onFinish: () -> Unit) {
        scope.launch {
            animatable.apply {
                val result = animateTo(0f, endSpec) {
                    state.animatableValue = value
                }
                if (result.endReason == AnimationEndReason.Finished) {
                    onFinish()
                }
            }
        }
    }

    private fun stopAnimation(reset: Boolean) {
        scope.launch {
            if (reset) {
                animatable.snapTo(0f)
            } else {
                animatable.stop()
            }
        }
    }

    override fun handleDragStart(
        dragTargetKey: Any?,
        dataToDrop: Any?,
        offsetInBox: Offset,
        dragStartOffset: Offset,
        content: @Composable () -> Unit,
        contentSizePx: IntSize
    ) {
        stopAnimation(false)
        super.handleDragStart(dragTargetKey, dataToDrop, offsetInBox, dragStartOffset, content, contentSizePx)
        startDragStartAnimation()
    }

    override fun handleDragEnd() {
        stopAnimation(true)
        super.handleDragEnd()
    }

    override fun handleDragCancel() {
        startDragCancelAnimation {
            super.handleDragCancel()
        }
    }

    override fun calculateTargetOffset(): Offset {
        if (animatable.isRunning) {
            val offset = super.calculateTargetOffset() - state.dragTargetOffsetInBox
            return state.dragTargetOffsetInBox + offset * currentAnimatedValue()
        }
        return super.calculateTargetOffset()
    }

    private fun currentAnimatedValue(): Float {
        return state.animatableValue
    }
}