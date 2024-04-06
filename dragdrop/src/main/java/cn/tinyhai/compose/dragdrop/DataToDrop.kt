package cn.tinyhai.compose.dragdrop

interface DataToDrop<T> {
    val type: Class<T>
    fun data(): T?
}

class DataToDropWrapper<T>(
    override val type: Class<T>,
    private val dataProvider: () -> T?
) : DataToDrop<T> {
    override fun data(): T? {
        return type.cast(dataProvider())
    }
}