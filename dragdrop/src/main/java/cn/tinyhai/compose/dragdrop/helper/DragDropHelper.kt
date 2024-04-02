package cn.tinyhai.compose.dragdrop.helper

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import cn.tinyhai.compose.dragdrop.AnimatedDragTargetInfo
import cn.tinyhai.compose.dragdrop.SimpleDragTargetInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "DragDropHelper"

interface DragDropHelper {
    fun handleDragStart(
        dataToDrop: Any?,
        dragStartOffset: Offset,
        dragTargetBoundInBox: Rect,
        content: @Composable () -> Unit
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
        dataToDrop: Any?,
        dragStartOffset: Offset,
        dragTargetBoundInBox: Rect,
        content: @Composable () -> Unit
    ) {
        state.apply {
            isDragging = true
            dragOffset = dragStartOffset
            this.dataToDrop = dataToDrop
            dragTargetContent = content
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
        dataToDrop: Any?,
        dragStartOffset: Offset,
        dragTargetBoundInBox: Rect,
        content: @Composable () -> Unit
    ) {
        stopAnimation(false) // when we drag the same dragTarget, just stop it but dont snap to zero
        super.handleDragStart(dataToDrop, dragStartOffset, dragTargetBoundInBox, content)
        startDragStartAnimation()
    }

    override fun handleDragEnd() {
        stopAnimation(true)
        super.handleDragEnd()
    }

    override fun handleDragCancel(reset: () -> Unit) {
        startDragCancelAnimation {
            super.handleDragCancel(reset)
        }
    }

    override fun calculateTargetOffset(): Offset {
        if (animatable.isRunning) {
            val offset = super.calculateTargetOffset() - state.dragTargetBoundInBox.topLeft
            Log.d(TAG, offset.toString())
            return state.dragTargetBoundInBox.topLeft + offset * currentAnimatedValue()
        }
        return super.calculateTargetOffset()
    }

    private fun currentAnimatedValue(): Float {
        return state.animatableValue
    }
}