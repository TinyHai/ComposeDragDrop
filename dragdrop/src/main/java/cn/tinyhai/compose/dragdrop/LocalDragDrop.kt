package cn.tinyhai.compose.dragdrop

import androidx.compose.runtime.compositionLocalOf

val LocalDragDrop = compositionLocalOf<DragDropState> { error("LocalDragDrop not present") }