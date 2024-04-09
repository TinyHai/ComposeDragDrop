package cn.tinyhai.compose.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import cn.tinyhai.compose.dragdrop.modifier.dropTarget

private const val TAG = "DropTarget"

interface DropTargetCallback<T> {

    val isInBound: Boolean

    fun isInterest(dataToDrop: DataToDrop<*>): Boolean

    fun contains(position: Offset): Boolean

    fun onDragIn(dataToDrop: DataToDrop<T>)

    fun onDragOut()

    fun onDrop(dataToDrop: DataToDrop<T>)

    fun onReset()
}

class DropTargetState<T>(
    private val type: Class<T>,
    private val onDrop: ((T?) -> Unit)?,
) : DropTargetCallback<T> {

    var boundInBox: Rect = Rect.Zero
    override var isInBound by mutableStateOf(false)
        private set

    var dataProvider: (() -> T?)? = null

    override fun isInterest(dataToDrop: DataToDrop<*>): Boolean {
        return type.isAssignableFrom(dataToDrop.type)
    }

    override fun contains(position: Offset): Boolean {
        return boundInBox.contains(position)
    }

    override fun onDragOut() {
        isInBound = false
        dataProvider = null
    }

    override fun onReset() {
        isInBound = false
        dataProvider = null
    }

    override fun onDrop(dataToDrop: DataToDrop<T>) {
        if (type.isAssignableFrom(dataToDrop.type)) {
            onDrop?.invoke(dataToDrop.data())
        }
    }

    override fun onDragIn(dataToDrop: DataToDrop<T>) {
        if (type.isAssignableFrom(dataToDrop.type)) {
            isInBound = true
            dataProvider = { dataToDrop.data() }
        }
    }

    operator fun component1() = isInBound

    operator fun component2() = dataProvider?.invoke()

    override fun toString(): String {
        return "DropTargetState(isInBound=$isInBound, dataToDrop=${dataProvider?.invoke()})"
    }
}

@Composable
inline fun <reified T> rememberDropTargetState(noinline onDrop: (T?) -> Unit): DropTargetState<T> {
    return rememberDropTargetState(T::class.java, onDrop)
}

@Composable
fun <T> rememberDropTargetState(type: Class<T>, onDrop: (T?) -> Unit): DropTargetState<T> {
    val curOnDrop by rememberUpdatedState(newValue = onDrop)
    return remember(type) {
        DropTargetState(type) { curOnDrop(it) }
    }
}

@Composable
inline fun <reified T> DropTarget(
    noinline onDrop: (T?) -> Unit,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    content: @Composable BoxScope.(isInBound: Boolean, data: T?) -> Unit
) {
    val state = rememberDropTargetState(onDrop)
    Box(modifier = modifier.dropTarget(state, enable)) {
        content(state.isInBound, state.dataProvider?.invoke())
    }
}

@Composable
fun <T> DropTarget(
    state: DropTargetState<T>,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    content: BoxScope.(isInBound: Boolean, data: T?) -> Unit
) {
    Box(modifier = modifier.dropTarget(state, enable)) {
        content(state.isInBound, state.dataProvider?.invoke())
    }
}