package cn.tinyhai.compose.dragdrop.helper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.drawscope.DrawScope
import cn.tinyhai.compose.dragdrop.AnimatedDragTargetInfo
import cn.tinyhai.compose.dragdrop.DataToDrop
import cn.tinyhai.compose.dragdrop.SimpleDragTargetInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "DragDropHelper"

interface DragDropHelper {
    fun handleDragStart(
        dataToDrop: DataToDrop<Any?>,
        dragStartOffset: Offset,
        dragTargetSnapshot: DrawScope.() -> Unit,
        dragTargetBoundInBox: Rect,
    )

    fun handleDrag(dragOffset: Offset)
    fun handleDragEnd()
    fun handleDragCancel(reset: () -> Unit)
    fun calculateTargetOffset(): Offset
    fun currentDragOffset(): Offset
}

internal open class SimpleDragDropHelper(
    private val state: SimpleDragTargetInfo
) : DragDropHelper {

    override fun handleDragStart(
        dataToDrop: DataToDrop<Any?>,
        dragStartOffset: Offset,
        dragTargetSnapshot: DrawScope.() -> Unit,
        dragTargetBoundInBox: Rect,
    ) {
        state.apply {
            isDragging = true
            dragOffset = dragStartOffset
            this.dataToDrop = dataToDrop
            this.dragTargetSnapshot = dragTargetSnapshot
            this.dragTargetBoundInBox = dragTargetBoundInBox
        }
    }

    override fun handleDrag(dragOffset: Offset) {
        state.dragOffset = dragOffset
    }

    override fun handleDragEnd() {
        reset()
    }

    override fun handleDragCancel(reset: () -> Unit) {
        this.reset()
        reset()
    }

    override fun calculateTargetOffset(): Offset {
        return currentDragOffset() - state.dragTargetBoundInBox.size.center
    }

    override fun currentDragOffset(): Offset {
        return state.dragOffset
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
        scope.launch {
            snapshotFlow { animatable.isRunning }.collectLatest { state.isAnimationRunning = it }
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
                val result = animateTo(0f, endSpec)
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
        dataToDrop: DataToDrop<Any?>,
        dragStartOffset: Offset,
        dragTargetSnapshot: DrawScope.() -> Unit,
        dragTargetBoundInBox: Rect,
    ) {
        stopAnimation(false) // when we drag the same dragTarget, just stop it but dont snap to zero
        super.handleDragStart(dataToDrop, dragStartOffset, dragTargetSnapshot, dragTargetBoundInBox)
        startDragStartAnimation()
    }

    override fun handleDragEnd() {
        stopAnimation(true)
        super.handleDragEnd()
    }

    override fun handleDragCancel(reset: () -> Unit) {
        state.isDragging = false
        startDragCancelAnimation {
            super.handleDragCancel(reset)
        }
    }

    override fun calculateTargetOffset(): Offset {
        if (state.isAnimationRunning) {
            val offset = super.calculateTargetOffset() - state.dragTargetBoundInBox.topLeft
            return state.dragTargetBoundInBox.topLeft + offset * currentAnimatedValue()
        }
        return super.calculateTargetOffset()
    }

    private fun currentAnimatedValue(): Float {
        return state.animatableValue
    }
}