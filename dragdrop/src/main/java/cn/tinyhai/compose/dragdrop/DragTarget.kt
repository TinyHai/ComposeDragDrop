package cn.tinyhai.compose.dragdrop

import android.graphics.Picture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import cn.tinyhai.compose.dragdrop.modifier.dragTarget

private const val TAG = "DragTarget"

interface DragTargetCallback<T> {
    val dataToDrop: DataToDrop<T>
    val boundInBox: Rect

    val snapshot: DrawScope.() -> Unit

    fun contains(position: Offset): Boolean

    fun onDragStart()

    fun onDragEnd()

    fun onReset()
}

class DragTargetState<T>(
    override val dataToDrop: DataToDrop<T>,
) : DragTargetCallback<T> {

    var isDragging: Boolean by mutableStateOf(false)

    override var boundInBox: Rect = Rect.Zero

    var picture: Picture? = null

    override val snapshot: DrawScope.() -> Unit = {
        picture?.let {
            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(it) }
        }
    }

    override fun onDragStart() {
        isDragging = true
    }

    override fun onDragEnd() {
        isDragging = false
    }

    override fun onReset() {
        isDragging = false
    }

    override fun contains(position: Offset): Boolean {
        return boundInBox.contains(position)
    }

    override fun toString(): String {
        return "DragTargetState(dataToDrop: $dataToDrop, isDragging: $isDragging, boundInBox: $boundInBox)"
    }
}

@Composable
fun <T> rememberDragTargetState(
    type: Class<T>,
    dataToDrop: T?
): DragTargetState<T> {
    return rememberDragTargetState(type, dataProvider = { dataToDrop })
}

@Composable
inline fun <reified T> rememberDragTargetState(
    dataToDrop: T?,
): DragTargetState<T> {
    return rememberDragTargetState(T::class.java, dataToDrop)
}

@Composable
inline fun <reified T> rememberDragTargetState(
    noinline dataProvider: () -> T?
): DragTargetState<T> {
    return rememberDragTargetState(T::class.java, dataProvider)
}

@Composable
fun <T> rememberDragTargetState(
    type: Class<T>,
    dataProvider: () -> T?,
): DragTargetState<T> {
    val currentDataProvider by rememberUpdatedState(dataProvider)
    val wrapper = remember {
        DataToDropWrapper(type) { currentDataProvider() }
    }
    return remember(wrapper) {
        DragTargetState(wrapper)
    }
}

@Composable
inline fun <reified T> DragTarget(
    dataToDrop: T?,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    hiddenWhileDragging: Boolean = false,
    content: @Composable () -> Unit
) {
    val state = rememberDragTargetState(dataToDrop)
    Box(
        modifier = modifier
            .dragTarget(
                enable = enable,
                state = state,
                hiddenWhileDragging = hiddenWhileDragging,
            )
    ) {
        content()
    }
}

@Composable
fun <T> DragTarget(
    state: DragTargetState<T>,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .dragTarget(
                state = state,
                enable = enable,
            )
    ) {
        content()
    }
}